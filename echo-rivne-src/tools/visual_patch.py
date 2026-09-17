from pathlib import Path
p=Path('echo-rivne-src/scripts/Main.gd')
s=p.read_text(encoding='utf-8')

def rep(old,new,name):
    global s
    if old not in s:
        raise SystemExit(f'MISSING PATCH TARGET: {name}')
    s=s.replace(old,new,1)
    print('patched',name)

rep('var world_time = 18.2','var world_time = 15.8','start daylight')

rep('''func make_mat(c, glow = Color(0,0,0)):
    var m = SpatialMaterial.new()
    m.albedo_color = c
    m.roughness = 0.75
    if glow != Color(0,0,0):
        m.emission_enabled = true
        m.emission = glow
        m.emission_energy = 1.6
    return m
''','''func make_mat(c, glow = Color(0,0,0)):
    var m = SpatialMaterial.new()
    m.albedo_color = c
    m.roughness = 0.88
    # Stable stylized rendering on old GLES2 phones: do not depend on dynamic light.
    m.flags_unshaded = true
    if glow != Color(0,0,0):
        m.emission_enabled = true
        m.emission = glow
        m.emission_energy = 1.2
    return m
''','unshaded materials')

rep('''func setup_world():
    var we = WorldEnvironment.new()
    env = Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.08,0.12,0.18)
    env.ambient_light_enabled = true
    env.ambient_light_color = Color(0.55,0.62,0.72)
    env.ambient_light_energy = 0.55
    env.fog_enabled = true
    env.fog_color = Color(0.2,0.24,0.3)
    env.fog_depth_begin = 100
    env.fog_depth_end = 330
    we.environment = env
    add_child(we)
    sun = DirectionalLight.new()
    sun.rotation_degrees = Vector3(-45,-35,0)
    sun.light_energy = 1.0
    sun.shadow_enabled = true
    add_child(sun)
    add_box(self,"GROUND",Vector3(0,-1,0),Vector3(470,2,470),Color(0.13,0.18,0.14),true)
    add_box(self,"GROUND_VIS",Vector3(0,0,0),Vector3(470,0.04,470),Color(0.15,0.21,0.16),false)

func road(pos,size):
    add_box(self,"ROAD",pos,size,Color(0.05,0.055,0.06),false)

func building(pos,size,color,lit = false):
    add_box(self,"BUILDING",pos+Vector3(0,size.y*0.5,0),size,color,true)
    var cols = int(clamp(size.x/8.0,1,6))
    var rows = int(clamp(size.y/6.0,1,6))
    for y in range(rows):
        for x in range(cols):
            if (x+y)%2 == 0:
                var px = pos.x-size.x*0.5+3+x*(size.x/max(cols,1))
                var glow = Color(0.9,0.55,0.2) if lit else Color(0,0,0)
                add_box(self,"WIN",Vector3(px,3+y*5,pos.z-size.z*0.5-0.04),Vector3(2,1.5,0.08),Color(0.18,0.22,0.26),false,glow)
''','''func setup_world():
    var we = WorldEnvironment.new()
    env = Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.38,0.55,0.72)
    env.ambient_light_enabled = true
    env.ambient_light_color = Color(1.0,1.0,1.0)
    env.ambient_light_energy = 1.0
    env.fog_enabled = false
    we.environment = env
    add_child(we)
    sun = DirectionalLight.new()
    sun.rotation_degrees = Vector3(-52,-35,0)
    sun.light_color = Color(1.0,0.88,0.70)
    sun.light_energy = 0.55
    sun.shadow_enabled = false
    add_child(sun)
    add_box(self,"GROUND",Vector3(0,-1.2,0),Vector3(470,2.4,470),Color(0.18,0.30,0.19),true)
    add_box(self,"GROUND_VIS",Vector3(0,0,0),Vector3(470,0.04,470),Color(0.22,0.39,0.23),false)

func road(pos,size):
    add_box(self,"ROAD",pos,size,Color(0.12,0.13,0.15),false)
    if size.x > size.z:
        add_box(self,"SIDEWALK",Vector3(pos.x,0.05,pos.z-size.z*0.5-2.2),Vector3(size.x,0.10,3.8),Color(0.55,0.53,0.49),false)
        add_box(self,"SIDEWALK",Vector3(pos.x,0.05,pos.z+size.z*0.5+2.2),Vector3(size.x,0.10,3.8),Color(0.55,0.53,0.49),false)
        for px in range(int(pos.x-size.x*0.5)+8,int(pos.x+size.x*0.5)-8,16):
            add_box(self,"LANE",Vector3(px,0.05,pos.z),Vector3(6,0.02,0.18),Color(0.88,0.82,0.60),false)
    else:
        add_box(self,"SIDEWALK",Vector3(pos.x-size.x*0.5-2.2,0.05,pos.z),Vector3(3.8,0.10,size.z),Color(0.55,0.53,0.49),false)
        add_box(self,"SIDEWALK",Vector3(pos.x+size.x*0.5+2.2,0.05,pos.z),Vector3(3.8,0.10,size.z),Color(0.55,0.53,0.49),false)
        for pz in range(int(pos.z-size.z*0.5)+8,int(pos.z+size.z*0.5)-8,16):
            add_box(self,"LANE",Vector3(pos.x,0.05,pz),Vector3(0.18,0.02,6),Color(0.88,0.82,0.60),false)

func building(pos,size,color,lit = false):
    add_box(self,"BUILDING",pos+Vector3(0,size.y*0.5,0),size,color,true)
    add_box(self,"ROOF",pos+Vector3(0,size.y+0.25,0),Vector3(size.x+0.4,0.5,size.z+0.4),Color(color.r*0.55,color.g*0.55,color.b*0.55),false)
    var cols = int(clamp(size.x/7.0,2,7))
    var rows = int(clamp(size.y/5.0,1,7))
    for y in range(rows):
        for x in range(cols):
            if (x+y)%2 == 0:
                var px = pos.x-size.x*0.5+3+x*(size.x/max(cols,1))
                var wc = Color(0.16,0.28,0.34)
                var glow = Color(0,0,0)
                if lit and (x+y)%3==0:
                    wc = Color(0.95,0.67,0.28)
                    glow = Color(0.82,0.42,0.10)
                add_box(self,"WIN",Vector3(px,3+y*4.4,pos.z-size.z*0.5-0.04),Vector3(2,1.5,0.08),wc,false,glow)
''','bright world geometry')

start=s.index('func make_human(parent,primary,accent,index = 0):')
end=s.index('\nfunc spawn_player():',start)
new='''func make_human(parent,primary,accent,index = 0):
    var visual = Spatial.new()
    visual.name = "Visual"
    parent.add_child(visual)
    var torso = Spatial.new()
    torso.name = "Torso"
    torso.translation = Vector3(0,1.18,0)
    visual.add_child(torso)
    add_box(torso,"Chest",Vector3(0,0,0),Vector3(0.72,0.94,0.38),primary,false)
    add_box(torso,"Jacket",Vector3(0,0.08,-0.205),Vector3(0.76,0.54,0.05),accent,false)
    var head = Spatial.new()
    head.name = "Head"
    head.translation = Vector3(0,0.77,0)
    torso.add_child(head)
    var hm = MeshInstance.new()
    var hs = SphereMesh.new()
    hs.radius = 0.27
    hs.height = 0.54
    hm.mesh = hs
    hm.material_override = make_mat(Color(0.91,0.76,0.62))
    head.add_child(hm)
    add_box(head,"Hair",Vector3(0,0.22,0.02),Vector3(0.46,0.12,0.43),Color(0.12,0.08,0.06),false)
    make_limb(visual,"ArmL",Vector3(-0.45,1.50,0),Vector3(0.18,0.72,0.18),primary)
    make_limb(visual,"ArmR",Vector3(0.45,1.50,0),Vector3(0.18,0.72,0.18),primary)
    make_limb(visual,"LegL",Vector3(-0.18,0.74,0),Vector3(0.20,0.82,0.22),Color(0.10,0.12,0.16))
    make_limb(visual,"LegR",Vector3(0.18,0.74,0),Vector3(0.20,0.82,0.22),Color(0.10,0.12,0.16))
    add_box(visual,"ShoeL",Vector3(-0.18,0.05,-0.08),Vector3(0.24,0.12,0.38),Color(0.04,0.04,0.05),false)
    add_box(visual,"ShoeR",Vector3(0.18,0.05,-0.08),Vector3(0.24,0.12,0.38),Color(0.04,0.04,0.05),false)
'''
s=s[:start]+new+s[end:]
print('patched human proportions')

rep('''    cam.translation = Vector3(0,6.0,9.8)
    cam.rotation_degrees = Vector3(-24,0,0)
    cam.current = true
''','''    cam.translation = Vector3(0,3.15,6.2)
    cam.rotation_degrees = Vector3(-14,0,0)
    cam.fov = 70.0
    cam.current = true
''','player camera')
rep('''    cam.translation = Vector3(0,5.3,10)
    cam.rotation_degrees = Vector3(-18,0,0)
''','''    cam.translation = Vector3(0,3.0,6.8)
    cam.rotation_degrees = Vector3(-12,0,0)
    cam.fov = 72.0
''','car camera')

rep('''func add_btn(parent,text,pos,size,method,args = []):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("pressed",self,method,args)
    parent.add_child(b)
    return b
''','''func add_btn(parent,text,pos,size,method,args = []):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    var normal = StyleBoxFlat.new()
    normal.bg_color = Color(0.06,0.10,0.13,0.94)
    normal.corner_radius_top_left = 8
    normal.corner_radius_top_right = 8
    normal.corner_radius_bottom_left = 8
    normal.corner_radius_bottom_right = 8
    b.add_stylebox_override("normal",normal)
    var pressed = normal.duplicate()
    pressed.bg_color = Color(0.02,0.62,0.62,0.96)
    b.add_stylebox_override("pressed",pressed)
    b.add_color_override("font_color",Color(0.90,0.98,0.96))
    b.connect("pressed",self,method,args)
    parent.add_child(b)
    return b
''','styled buttons')

rep('menu_root.color = Color(0.005,0.015,0.025,0.94)','menu_root.color = Color(0.025,0.055,0.075,0.97)','menu brightness')
rep('scanline.color = Color(0,0.9,0.8,0.10)','scanline.color = Color(0.0,0.95,0.82,0.13)','menu scanline')

s=s.replace('Color(0.3,0.31,0.31)','Color(0.66,0.64,0.59)')
s=s.replace('Color(0.65,0.59,0.48)','Color(0.72,0.62,0.49)')
s=s.replace('Color(0.08,0.25,0.12)','Color(0.16,0.43,0.22)')
s=s.replace('Color(0.04,0.19,0.28)','Color(0.07,0.38,0.52)')
s=s.replace('Color(0.04,0.17,0.26)','Color(0.07,0.34,0.48)')

p.write_text(s,encoding='utf-8')
print('visual patch complete, lines',len(s.splitlines()))
