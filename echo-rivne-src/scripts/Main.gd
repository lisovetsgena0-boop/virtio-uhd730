extends Spatial

const PlayerScript = preload("res://scripts/Player.gd")
const CarScript = preload("res://scripts/Car.gd")
const NPCScript = preload("res://scripts/NPC.gd")
const TrafficScript = preload("res://scripts/TrafficCar.gd")
const Save = preload("res://scripts/SaveSystem.gd")

var player
var car
var active_actor
var in_car = false
var game_started = false
var money = 1200
var reputation = 0
var echo_level = 0
var mission_stage = 0
var world_time = 18.2
var day = 1
var weather = "CLEAR"
var inventory = {"water":1,"medkit":0,"echo_chip":0}
var collected = {}
var settings = {"music":0.65,"sfx":0.85,"touch":true,"quality":1}

var env
var sun
var ui
var hud
var mission
var hint
var toast
var phone_panel
var inventory_panel
var map_panel
var shop_panel
var dialogue_panel
var menu_root
var settings_panel
var cutscene_panel
var cutscene_text
var menu_title
var scanline
var music
var ambient
var sfx
var touch_controls = []
var overlay_open = false
var cutscene_time = 0.0

var player_spawn = Vector3(0,1.8,28)
var car_spawn = Vector3(24,0.9,22)
var LOC = {
    "GARAGE":Vector3(38,0,48),
    "ECHO":Vector3(-155,0,-177),
    "SHOP":Vector3(-34,0,78),
    "GAS":Vector3(118,0,78),
    "CAFE":Vector3(-62,0,-2)
}

func _ready():
    randomize()
    setup_world()
    build_city()
    spawn_player()
    spawn_car()
    spawn_npcs()
    spawn_traffic()
    setup_audio()
    build_ui()
    load_settings()
    load_game()
    apply_settings()
    open_menu(true)

func _process(delta):
    animate_menu()
    if not game_started:
        return
    if cutscene_panel.visible:
        cutscene_time += delta
        update_cutscene()
        return
    if Input.is_action_just_pressed("phone"):
        toggle_phone()
    if Input.is_action_just_pressed("inventory"):
        toggle_inventory()
    if Input.is_action_just_pressed("map"):
        toggle_map()
    if Input.is_action_just_pressed("vehicle"):
        toggle_vehicle()
    if Input.is_action_just_pressed("interact"):
        interact()
    if Input.is_action_just_pressed("save_game"):
        save_game()
    world_time += delta * 0.012
    if world_time >= 24.0:
        world_time -= 24.0
        day += 1
        if day % 3 == 0:
            weather = "RAIN" if weather == "CLEAR" else "CLEAR"
    update_environment()
    update_hud()
    update_interaction()
    check_echo()
    failsafe()

func make_mat(c, glow = Color(0,0,0)):
    var m = SpatialMaterial.new()
    m.albedo_color = c
    m.roughness = 0.75
    if glow != Color(0,0,0):
        m.emission_enabled = true
        m.emission = glow
        m.emission_energy = 1.6
    return m

func add_box(parent, name, pos, size, color, collision = true, glow = Color(0,0,0)):
    var node = StaticBody.new() if collision else Spatial.new()
    node.name = name
    node.translation = pos
    parent.add_child(node)
    var mi = MeshInstance.new()
    var cm = CubeMesh.new()
    cm.size = size
    mi.mesh = cm
    mi.material_override = make_mat(color,glow)
    node.add_child(mi)
    if collision:
        var cs = CollisionShape.new()
        var bs = BoxShape.new()
        bs.extents = size * 0.5
        cs.shape = bs
        node.add_child(cs)
    return node

func setup_world():
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

func build_city():
    road(Vector3(0,0.03,0),Vector3(400,0.05,20))
    road(Vector3(0,0.03,76),Vector3(330,0.05,14))
    road(Vector3(0,0.03,-82),Vector3(330,0.05,14))
    road(Vector3(-100,0.03,0),Vector3(14,0.05,330))
    road(Vector3(100,0.03,0),Vector3(14,0.05,330))
    add_box(self,"THEATRE_SQUARE",Vector3(-8,0.04,-27),Vector3(78,0.08,42),Color(0.3,0.31,0.31),false)
    building(Vector3(-8,0,-58),Vector3(48,17,20),Color(0.65,0.59,0.48),true)
    for x in [-20,-12,-4,4,12]:
        add_box(self,"COLUMN",Vector3(x,9,-47.7),Vector3(2,11,2),Color(0.82,0.77,0.66),false)
    var blocks = [
        [Vector3(-152,0,-36),Vector3(38,18,34),Color(0.46,0.44,0.41)],
        [Vector3(-122,0,36),Vector3(44,14,34),Color(0.54,0.50,0.44)],
        [Vector3(-65,0,-38),Vector3(38,15,34),Color(0.58,0.54,0.47)],
        [Vector3(48,0,-38),Vector3(54,22,34),Color(0.39,0.42,0.47)],
        [Vector3(54,0,37),Vector3(52,17,34),Color(0.51,0.51,0.50)],
        [Vector3(135,0,-38),Vector3(52,16,34),Color(0.60,0.53,0.46)]
    ]
    for b in blocks:
        building(b[0],b[1],b[2],true)
    add_box(self,"LEBEDYNKA",Vector3(72,0.03,112),Vector3(115,0.05,58),Color(0.08,0.25,0.12),false)
    add_box(self,"WATER",Vector3(72,0.07,112),Vector3(52,0.04,28),Color(0.04,0.19,0.28),false)
    add_box(self,"BASIV_WATER",Vector3(-87,0.05,173),Vector3(180,0.04,64),Color(0.04,0.17,0.26),false)
    for i in range(12):
        building(Vector3(120+(i%4)*27,0,-172+int(i/4)*42),Vector3(20,25+(i%3)*7,31),Color(0.4,0.42,0.46),i%4==0)
    building(Vector3(-155,0,-150),Vector3(78,12,48),Color(0.23,0.26,0.28),false)
    add_box(self,"ECHO_TOWER",Vector3(-155,24,-150),Vector3(14,36,14),Color(0.07,0.09,0.11),true)
    add_box(self,"ECHO_CORE",LOC["ECHO"]+Vector3(0,1.5,0),Vector3(5,3,5),Color(0.03,0.17,0.18),false,Color(0,0.95,0.82))
    building(LOC["SHOP"],Vector3(15,6,13),Color(0.28,0.34,0.39),true)
    building(LOC["GAS"],Vector3(18,5,12),Color(0.34,0.32,0.28),true)
    building(LOC["CAFE"],Vector3(18,7,14),Color(0.38,0.24,0.20),true)
    var frags = [Vector3(-8,1,-25),Vector3(72,1,112),Vector3(-110,1,145),Vector3(150,1,-150),Vector3(-155,1,-178),Vector3(38,1,48)]
    for i in range(frags.size()):
        spawn_fragment(i,frags[i])

func spawn_fragment(id,pos):
    var a = Area.new()
    a.name = "ECHO_%d" % id
    a.translation = pos
    a.set_meta("echo_id",id)
    add_child(a)
    var cs = CollisionShape.new()
    var ss = SphereShape.new()
    ss.radius = 1.5
    cs.shape = ss
    a.add_child(cs)
    var mi = MeshInstance.new()
    var sp = SphereMesh.new()
    sp.radius = 0.5
    sp.height = 1.0
    mi.mesh = sp
    mi.material_override = make_mat(Color(0.02,0.32,0.35),Color(0,1,0.82))
    a.add_child(mi)

func make_limb(parent,name,pos,size,color):
    var pivot = Spatial.new()
    pivot.name = name
    pivot.translation = pos
    parent.add_child(pivot)
    var mi = MeshInstance.new()
    var cm = CubeMesh.new()
    cm.size = size
    mi.mesh = cm
    mi.translation = Vector3(0,-size.y*0.5,0)
    mi.material_override = make_mat(color)
    pivot.add_child(mi)

func make_human(parent,primary,accent,index = 0):
    var visual = Spatial.new()
    visual.name = "Visual"
    parent.add_child(visual)
    var torso = Spatial.new()
    torso.name = "Torso"
    torso.translation = Vector3(0,1.0,0)
    visual.add_child(torso)
    var tm = MeshInstance.new()
    var tb = CubeMesh.new()
    tb.size = Vector3(0.85,1.1,0.45)
    tm.mesh = tb
    tm.material_override = make_mat(primary)
    torso.add_child(tm)
    var head = Spatial.new()
    head.name = "Head"
    head.translation = Vector3(0,1.0,0)
    torso.add_child(head)
    var hm = MeshInstance.new()
    var hs = SphereMesh.new()
    hs.radius = 0.32
    hs.height = 0.64
    hm.mesh = hs
    hm.material_override = make_mat(Color(0.91,0.78,0.66))
    head.add_child(hm)
    make_limb(visual,"ArmL",Vector3(-0.53,1.42,0),Vector3(0.22,0.82,0.22),accent)
    make_limb(visual,"ArmR",Vector3(0.53,1.42,0),Vector3(0.22,0.82,0.22),accent)
    make_limb(visual,"LegL",Vector3(-0.2,0.47,0),Vector3(0.24,0.92,0.24),Color(0.12,0.14,0.18))
    make_limb(visual,"LegR",Vector3(0.2,0.47,0),Vector3(0.24,0.92,0.24),Color(0.12,0.14,0.18))

func spawn_player():
    player = KinematicBody.new()
    player.name = "Player"
    player.script = PlayerScript
    player.translation = player_spawn
    add_child(player)
    var cs = CollisionShape.new()
    var cap = CapsuleShape.new()
    cap.radius = 0.42
    cap.height = 0.95
    cs.shape = cap
    player.add_child(cs)
    make_human(player,Color(0.1,0.58,0.82),Color(0.9,0.93,0.96))
    var cam = Camera.new()
    cam.name = "Camera"
    cam.translation = Vector3(0,6.0,9.8)
    cam.rotation_degrees = Vector3(-24,0,0)
    cam.current = true
    player.add_child(cam)
    player.spawn_point = player_spawn
    player.enabled = false
    active_actor = player

func spawn_car():
    car = KinematicBody.new()
    car.name = "Car"
    car.script = CarScript
    car.translation = car_spawn
    add_child(car)
    var cs = CollisionShape.new()
    var bs = BoxShape.new()
    bs.extents = Vector3(1.15,0.6,2.25)
    cs.shape = bs
    car.add_child(cs)
    add_box(car,"Body",Vector3(0,0.68,0),Vector3(2.35,0.85,4.5),Color(0.1,0.23,0.4),false)
    add_box(car,"Cabin",Vector3(0,1.25,-0.1),Vector3(1.85,0.72,2.3),Color(0.07,0.1,0.14),false)
    for data in [["WheelFL",-1.12,-1.45],["WheelFR",1.12,-1.45],["WheelRL",-1.12,1.45],["WheelRR",1.12,1.45]]:
        var w = Spatial.new()
        w.name = data[0]
        w.translation = Vector3(data[1],0.38,data[2])
        car.add_child(w)
        add_box(w,"Tire",Vector3(),Vector3(0.34,0.74,0.74),Color(0.03,0.03,0.03),false)
    var cam = Camera.new()
    cam.name = "Camera"
    cam.translation = Vector3(0,5.3,10)
    cam.rotation_degrees = Vector3(-18,0,0)
    car.add_child(cam)
    car.spawn_point = car_spawn

func spawn_npcs():
    var centers = [Vector3(0,1.1,25),Vector3(70,1.1,92),Vector3(135,1.1,-95),Vector3(-100,1.1,40)]
    for i in range(20):
        var n = KinematicBody.new()
        n.name = "NPC_%02d" % i
        n.script = NPCScript
        var c = centers[i%centers.size()]
        n.translation = c + Vector3((i*7)%18-9,0,(i*11)%18-9)
        add_child(n)
        n.setup(c,6.0+(i%5)*2.0,float(i))
        var cs = CollisionShape.new()
        var cap = CapsuleShape.new()
        cap.radius = 0.32
        cap.height = 0.8
        cs.shape = cap
        n.add_child(cs)
        make_human(n,Color(0.25+0.08*(i%4),0.32+0.05*(i%3),0.45),Color(0.75,0.78,0.82),i)

func spawn_traffic():
    var routes = [
        [Vector3(-180,0.35,-5),Vector3(180,0.35,-5),Vector3(180,0.35,5),Vector3(-180,0.35,5)],
        [Vector3(-95,0.35,-145),Vector3(-95,0.35,145),Vector3(-105,0.35,145),Vector3(-105,0.35,-145)],
        [Vector3(95,0.35,145),Vector3(95,0.35,-145),Vector3(105,0.35,-145),Vector3(105,0.35,145)]
    ]
    for i in range(9):
        var t = Spatial.new()
        t.name = "Traffic_%02d" % i
        t.script = TrafficScript
        add_child(t)
        add_box(t,"Body",Vector3(0,0.55,0),Vector3(1.7,0.65,3.4),Color(0.2+0.08*(i%4),0.2,0.32+0.05*(i%3)),false)
        var r = routes[i%routes.size()].duplicate()
        for k in range(i%4):
            r.append(r.pop_front())
        t.setup(r,5.5+float(i%4))

func setup_audio():
    music = AudioStreamPlayer.new()
    music.stream = load("res://assets/audio/menu_theme.wav")
    music.connect("finished",self,"restart_music")
    add_child(music)
    ambient = AudioStreamPlayer.new()
    ambient.stream = load("res://assets/audio/city_loop.wav")
    ambient.connect("finished",self,"restart_ambient")
    add_child(ambient)
    sfx = AudioStreamPlayer.new()
    add_child(sfx)

func restart_music():
    if menu_root.visible:
        music.play()

func restart_ambient():
    if game_started and not menu_root.visible:
        ambient.play()

func play_sfx(kind):
    sfx.stream = load("res://assets/audio/pickup.wav") if kind == "pickup" else load("res://assets/audio/click.wav")
    sfx.play()

func build_ui():
    ui = CanvasLayer.new()
    add_child(ui)
    hud = Label.new()
    hud.rect_position = Vector2(16,14)
    hud.rect_size = Vector2(650,70)
    ui.add_child(hud)
    mission = Label.new()
    mission.anchor_left = 1
    mission.anchor_right = 1
    mission.rect_position = Vector2(-390,58)
    mission.rect_size = Vector2(370,80)
    mission.align = Label.ALIGN_RIGHT
    ui.add_child(mission)
    hint = Label.new()
    hint.anchor_left = 0.5
    hint.anchor_right = 0.5
    hint.anchor_top = 1
    hint.anchor_bottom = 1
    hint.rect_position = Vector2(-240,-90)
    hint.rect_size = Vector2(480,40)
    hint.align = Label.ALIGN_CENTER
    ui.add_child(hint)
    toast = Label.new()
    toast.anchor_left = 0.5
    toast.anchor_right = 0.5
    toast.rect_position = Vector2(-300,105)
    toast.rect_size = Vector2(600,45)
    toast.align = Label.ALIGN_CENTER
    ui.add_child(toast)
    phone_panel = make_panel(Vector2(420,560))
    inventory_panel = make_panel(Vector2(460,410))
    map_panel = make_panel(Vector2(720,500))
    shop_panel = make_panel(Vector2(480,420))
    dialogue_panel = make_panel(Vector2(820,170),true)
    cutscene_panel = ColorRect.new()
    cutscene_panel.color = Color(0,0,0,0.9)
    cutscene_panel.anchor_right = 1
    cutscene_panel.anchor_bottom = 1
    cutscene_panel.visible = false
    ui.add_child(cutscene_panel)
    cutscene_text = Label.new()
    cutscene_text.anchor_left = 0.5
    cutscene_text.anchor_right = 0.5
    cutscene_text.anchor_top = 0.5
    cutscene_text.anchor_bottom = 0.5
    cutscene_text.rect_position = Vector2(-340,-75)
    cutscene_text.rect_size = Vector2(680,150)
    cutscene_text.align = Label.ALIGN_CENTER
    cutscene_text.valign = Label.V_ALIGN_CENTER
    cutscene_text.autowrap = true
    cutscene_panel.add_child(cutscene_text)
    build_phone()
    build_inventory()
    build_map()
    build_shop()
    build_dialogue()
    build_touch()
    build_menu()

func make_panel(size,bottom = false):
    var p = ColorRect.new()
    p.color = Color(0.02,0.04,0.055,0.97)
    p.anchor_left = 0.5
    p.anchor_right = 0.5
    if bottom:
        p.anchor_top = 1
        p.anchor_bottom = 1
        p.rect_position = Vector2(-size.x*0.5,-size.y-20)
    else:
        p.anchor_top = 0.5
        p.anchor_bottom = 0.5
        p.rect_position = Vector2(-size.x*0.5,-size.y*0.5)
    p.rect_size = size
    p.visible = false
    ui.add_child(p)
    return p

func add_label(parent,text,pos,size):
    var l = Label.new()
    l.text = text
    l.rect_position = pos
    l.rect_size = size
    l.autowrap = true
    parent.add_child(l)
    return l

func add_btn(parent,text,pos,size,method,args = []):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("pressed",self,method,args)
    parent.add_child(b)
    return b

func build_phone():
    add_label(phone_panel,"ECHO//PHONE",Vector2(20,18),Vector2(380,30))
    var l = add_label(phone_panel,"",Vector2(20,58),Vector2(380,440))
    l.name = "Text"
    add_btn(phone_panel,"CLOSE",Vector2(20,505),Vector2(380,38),"toggle_phone")

func build_inventory():
    add_label(inventory_panel,"INVENTORY",Vector2(20,18),Vector2(420,30))
    var l = add_label(inventory_panel,"",Vector2(20,58),Vector2(420,250))
    l.name = "Text"
    add_btn(inventory_panel,"USE MEDKIT",Vector2(20,330),Vector2(200,42),"use_medkit")
    add_btn(inventory_panel,"CLOSE",Vector2(235,330),Vector2(200,42),"toggle_inventory")

func build_map():
    add_label(map_panel,"RIVNE / CITY MAP",Vector2(20,16),Vector2(680,28))
    add_box_ui(map_panel,Color(0.12,0.16,0.2),Vector2(40,70),Vector2(640,350))
    add_box_ui(map_panel,Color(0.04,0.58,0.55),Vector2(315,220),Vector2(20,20))
    add_label(map_panel,"CENTER",Vector2(340,215),Vector2(100,30))
    add_label(map_panel,"LEBEDYNKA",Vector2(490,110),Vector2(130,30))
    add_label(map_panel,"BASIV KUT",Vector2(90,340),Vector2(120,30))
    add_label(map_panel,"PIVNICHNYI",Vector2(500,320),Vector2(130,30))
    add_label(map_panel,"ECHO ZONE",Vector2(60,100),Vector2(130,30))
    add_btn(map_panel,"CLOSE",Vector2(275,445),Vector2(170,38),"toggle_map")

func add_box_ui(parent,color,pos,size):
    var r = ColorRect.new()
    r.color = color
    r.rect_position = pos
    r.rect_size = size
    parent.add_child(r)

func build_shop():
    add_label(shop_panel,"CITY SERVICES",Vector2(20,18),Vector2(440,30))
    var l = add_label(shop_panel,"",Vector2(20,55),Vector2(440,90))
    l.name = "Text"
    add_btn(shop_panel,"WATER  40 UAH",Vector2(20,160),Vector2(210,42),"buy_item",["water",40])
    add_btn(shop_panel,"MEDKIT  250 UAH",Vector2(250,160),Vector2(210,42),"buy_item",["medkit",250])
    add_btn(shop_panel,"CAR SERVICE  300 UAH",Vector2(20,220),Vector2(440,42),"buy_service")
    add_btn(shop_panel,"CLOSE",Vector2(20,340),Vector2(440,42),"close_shop")

func build_dialogue():
    var l = add_label(dialogue_panel,"",Vector2(20,18),Vector2(780,95))
    l.name = "Text"
    add_btn(dialogue_panel,"OK",Vector2(650,120),Vector2(150,35),"close_dialogue")

func build_touch():
    var v = get_viewport().size
    touch_controls.append(touch_btn("UP",Vector2(95,v.y-180),Vector2(72,72),"forward"))
    touch_controls.append(touch_btn("DN",Vector2(95,v.y-100),Vector2(72,72),"back"))
    touch_controls.append(touch_btn("LT",Vector2(18,v.y-100),Vector2(72,72),"left"))
    touch_controls.append(touch_btn("RT",Vector2(172,v.y-100),Vector2(72,72),"right"))
    touch_controls.append(touch_btn("RUN",Vector2(18,v.y-180),Vector2(72,72),"sprint"))
    touch_controls.append(action_btn("E",Vector2(v.x-230,v.y-102),Vector2(72,72),"interact"))
    touch_controls.append(action_btn("CAR",Vector2(v.x-150,v.y-102),Vector2(72,72),"toggle_vehicle"))
    touch_controls.append(action_btn("INV",Vector2(v.x-150,v.y-180),Vector2(72,62),"toggle_inventory"))
    touch_controls.append(action_btn("MAP",Vector2(v.x-70,v.y-180),Vector2(58,62),"toggle_map"))
    touch_controls.append(action_btn("PH",Vector2(v.x-70,v.y-102),Vector2(58,72),"toggle_phone"))
    for b in touch_controls:
        ui.add_child(b)

func touch_btn(text,pos,size,action):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("button_down",self,"touch_down",[action])
    b.connect("button_up",self,"touch_up",[action])
    return b

func action_btn(text,pos,size,method):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("pressed",self,method)
    return b

func build_menu():
    menu_root = ColorRect.new()
    menu_root.color = Color(0.005,0.015,0.025,0.94)
    menu_root.anchor_right = 1
    menu_root.anchor_bottom = 1
    ui.add_child(menu_root)
    scanline = ColorRect.new()
    scanline.color = Color(0,0.9,0.8,0.10)
    scanline.rect_size = Vector2(get_viewport().size.x,30)
    menu_root.add_child(scanline)
    menu_title = add_label(menu_root,"ECHO//RIVNE",Vector2(get_viewport().size.x*0.5-220,55),Vector2(440,55))
    menu_title.align = Label.ALIGN_CENTER
    var x = get_viewport().size.x*0.5-190
    add_btn(menu_root,"START",Vector2(x,230),Vector2(380,46),"new_game")
    add_btn(menu_root,"CONTINUE",Vector2(x,288),Vector2(380,46),"continue_game")
    add_btn(menu_root,"SETTINGS",Vector2(x,346),Vector2(380,46),"open_settings")
    add_btn(menu_root,"SAVE GAME",Vector2(x,404),Vector2(380,46),"save_game")
    settings_panel = ColorRect.new()
    settings_panel.color = Color(0.04,0.07,0.10,0.98)
    settings_panel.anchor_left = 0.5
    settings_panel.anchor_right = 0.5
    settings_panel.anchor_top = 0.5
    settings_panel.anchor_bottom = 0.5
    settings_panel.rect_position = Vector2(-220,-170)
    settings_panel.rect_size = Vector2(440,340)
    settings_panel.visible = false
    menu_root.add_child(settings_panel)
    add_label(settings_panel,"SETTINGS",Vector2(20,18),Vector2(400,30)).align = Label.ALIGN_CENTER
    add_label(settings_panel,"Music",Vector2(25,60),Vector2(120,25))
    var ms = HSlider.new()
    ms.min_value=0; ms.max_value=1; ms.step=0.01; ms.value=settings["music"]
    ms.rect_position=Vector2(25,88); ms.rect_size=Vector2(390,20)
    ms.connect("value_changed",self,"set_music")
    settings_panel.add_child(ms)
    add_label(settings_panel,"SFX",Vector2(25,120),Vector2(120,25))
    var ss = HSlider.new()
    ss.min_value=0; ss.max_value=1; ss.step=0.01; ss.value=settings["sfx"]
    ss.rect_position=Vector2(25,148); ss.rect_size=Vector2(390,20)
    ss.connect("value_changed",self,"set_sfx")
    settings_panel.add_child(ss)
    var cb = CheckBox.new()
    cb.text="Touch controls"; cb.pressed=settings["touch"]
    cb.rect_position=Vector2(25,195); cb.rect_size=Vector2(240,30)
    cb.connect("toggled",self,"set_touch")
    settings_panel.add_child(cb)
    var q = OptionButton.new()
    q.add_item("LOW",0); q.add_item("BALANCED",1); q.add_item("NEON",2)
    q.selected=int(settings["quality"])
    q.rect_position=Vector2(25,235); q.rect_size=Vector2(220,34)
    q.connect("item_selected",self,"set_quality")
    settings_panel.add_child(q)
    add_btn(settings_panel,"BACK",Vector2(270,285),Vector2(145,38),"close_settings")
    add_btn(menu_root,"MENU",Vector2(get_viewport().size.x-110,18),Vector2(90,38),"open_pause")

func animate_menu():
    if menu_root == null or not menu_root.visible:
        return
    var t = OS.get_ticks_msec()/1000.0
    scanline.rect_position.y = 50 + fmod(t*60.0,max(get_viewport().size.y-80.0,80.0))
    menu_title.rect_position.y = 55 + sin(t*1.7)*4

func open_menu(initial=false):
    menu_root.visible = true
    settings_panel.visible = false
    player.enabled = false
    close_overlays()
    for b in touch_controls:
        b.visible = false
    if not music.playing:
        music.play()
    if ambient.playing:
        ambient.stop()
    if not initial:
        play_sfx("click")

func close_menu():
    menu_root.visible = false
    game_started = true
    player.enabled = true
    if not ambient.playing:
        ambient.play()
    apply_settings()

func new_game():
    money=1200; reputation=0; echo_level=0; mission_stage=0; day=1; world_time=18.2; weather="CLEAR"
    inventory={"water":1,"medkit":0,"echo_chip":0}; collected={}
    player.respawn(player_spawn)
    car.respawn(car_spawn)
    in_car=false; player.visible=true; active_actor=player
    for n in get_children():
        if n is Area and n.has_meta("echo_id"):
            n.visible=true
    close_menu()
    cutscene_time=0
    cutscene_panel.visible=true
    cutscene_text.text="RIVNE. 2026.\nA message appears on your phone from your own number..."

func continue_game():
    close_menu()

func open_pause():
    open_menu(false)

func open_settings():
    settings_panel.visible=true

func close_settings():
    settings_panel.visible=false
    Save.save_settings(settings)

func set_music(v):
    settings["music"]=v
    apply_settings()

func set_sfx(v):
    settings["sfx"]=v
    apply_settings()

func set_touch(v):
    settings["touch"]=v
    apply_settings()

func set_quality(i):
    settings["quality"]=i
    apply_settings()

func update_cutscene():
    if cutscene_time > 2.5 and cutscene_time < 5.0:
        cutscene_text.text="UNKNOWN: The model failed again.\nFind the cyan signal near the city center."
    elif cutscene_time >= 5.0:
        cutscene_panel.visible=false
        player.enabled=true
        toast_msg("Mission started: find the first ECHO signal.")

func toggle_phone():
    if menu_root.visible: return
    var show = not phone_panel.visible
    close_overlays()
    phone_panel.visible=show
    overlay_open=show
    if show:
        phone_panel.get_node("Text").text="BALANCE: %d UAH\nREP: %d\nECHO: %d\nDAY: %d  %02d:%02d\nWEATHER: %s\n\nUNKNOWN:\nThe city remembers routes that have not happened yet.\nFind the signals." % [money,reputation,echo_level,day,int(world_time),int((world_time-int(world_time))*60),weather]

func toggle_inventory():
    if menu_root.visible: return
    var show = not inventory_panel.visible
    close_overlays()
    inventory_panel.visible=show
    overlay_open=show
    if show:
        inventory_panel.get_node("Text").text="Water: %d\nMedkits: %d\nECHO chips: %d\n\nChips are fragments of a hidden city prediction network." % [inventory.get("water",0),inventory.get("medkit",0),inventory.get("echo_chip",0)]

func toggle_map():
    if menu_root.visible: return
    var show = not map_panel.visible
    close_overlays()
    map_panel.visible=show
    overlay_open=show

func close_overlays():
    if phone_panel: phone_panel.visible=false
    if inventory_panel: inventory_panel.visible=false
    if map_panel: map_panel.visible=false
    if shop_panel: shop_panel.visible=false
    if dialogue_panel: dialogue_panel.visible=false
    overlay_open=false

func open_shop(kind):
    close_overlays()
    shop_panel.visible=true
    overlay_open=true
    shop_panel.get_node("Text").text="24/7 MARKET" if kind=="SHOP" else "CITY GAS & SERVICE"

func close_shop():
    shop_panel.visible=false
    overlay_open=false

func buy_item(item,price):
    if money < price:
        toast_msg("Not enough money.")
        return
    money -= price
    inventory[item]=int(inventory.get(item,0))+1
    play_sfx("pickup")
    toast_msg("Purchased: "+item)

func buy_service():
    if money < 300:
        toast_msg("Not enough money.")
        return
    money-=300
    car.speed=0
    toast_msg("Car serviced.")

func use_medkit():
    if int(inventory.get("medkit",0))<=0:
        toast_msg("No medkits.")
        return
    inventory["medkit"]-=1
    toast_msg("Medkit used.")
    toggle_inventory()

func open_dialogue(text):
    close_overlays()
    dialogue_panel.visible=true
    overlay_open=true
    dialogue_panel.get_node("Text").text=text

func close_dialogue():
    dialogue_panel.visible=false
    overlay_open=false

func nearest_npc(maxd):
    var best=null
    var d=maxd
    for n in get_children():
        if n.name.begins_with("NPC_"):
            var nd=player.global_transform.origin.distance_to(n.global_transform.origin)
            if nd<d:
                d=nd; best=n
    return best

func interact():
    if shop_panel.visible or dialogue_panel.visible:
        return
    if distance_to(LOC["SHOP"])<8.5:
        open_shop("SHOP"); return
    if distance_to(LOC["GAS"])<9:
        open_shop("GAS"); return
    if distance_to(LOC["CAFE"])<9:
        open_dialogue("BARTENDER: People say a camera near Soborna records events several minutes before they happen."); return
    var n=nearest_npc(2.8)
    if n!=null:
        var idx=int(n.name.replace("NPC_",""))
        var lines=["I saw a cyan light near Lebedynka last night.","Old servers under the city are still powered.","At Basiv Kut people hear phone notifications with no sender.","Do not follow ECHO markers after 02:13."]
        open_dialogue("CITIZEN: "+lines[idx%lines.size()]); return
    if distance_to(LOC["GARAGE"])<5:
        money+=350; reputation+=1; toast_msg("Garage shift complete. +350 UAH"); return
    if distance_to(LOC["ECHO"])<6:
        mission_stage=2; echo_level+=2; toast_msg("ECHO terminal synchronized."); return

func check_echo():
    for n in get_children():
        if n is Area and n.has_meta("echo_id") and n.visible:
            if player.global_transform.origin.distance_to(n.global_transform.origin)<2.6:
                var id=str(int(n.get_meta("echo_id")))
                if not collected.has(id):
                    collected[id]=true; n.visible=false; echo_level+=1; reputation+=1
                    inventory["echo_chip"]=int(inventory.get("echo_chip",0))+1
                    if mission_stage==0: mission_stage=1
                    play_sfx("pickup")
                    toast_msg("ECHO fragment found.")

func toggle_vehicle():
    if not in_car:
        if player.global_transform.origin.distance_to(car.global_transform.origin)>4:
            toast_msg("Move closer to the car."); return
        in_car=true; player.enabled=false; player.visible=false; car.occupied=true
        car.get_node("Camera").current=true; active_actor=car
    else:
        in_car=false; car.occupied=false
        player.global_transform.origin=car.global_transform.origin+car.global_transform.basis.x*2.4+Vector3(0,1,0)
        player.visible=true; player.enabled=true; player.get_node("Camera").current=true; active_actor=player

func touch_down(action):
    if in_car: car.set_touch(action,true)
    else: player.set_touch(action,true)

func touch_up(action):
    if in_car: car.set_touch(action,false)
    else: player.set_touch(action,false)

func distance_to(pos):
    return active_actor.global_transform.origin.distance_to(pos) if active_actor else 99999.0

func update_interaction():
    var s=""
    if not in_car and player.global_transform.origin.distance_to(car.global_transform.origin)<4: s="[F] ENTER CAR"
    if distance_to(LOC["SHOP"])<8.5: s="[E] 24/7 MARKET"
    if distance_to(LOC["GAS"])<9: s="[E] GAS / SERVICE"
    if distance_to(LOC["CAFE"])<9: s="[E] TALK IN CAFE"
    if nearest_npc(2.8)!=null: s="[E] TALK"
    if distance_to(LOC["GARAGE"])<5: s="[E] GARAGE JOB +350"
    if distance_to(LOC["ECHO"])<6: s="[E] ECHO TERMINAL"
    hint.text=s

func update_hud():
    hud.text="ECHO//RIVNE   %d UAH   REP %d   ECHO %d\nDAY %d   %02d:%02d   %s" % [money,reputation,echo_level,day,int(world_time),int((world_time-int(world_time))*60),weather]
    var obj="Find the first cyan ECHO signal."
    if mission_stage==1: obj="Investigate the western industrial zone."
    if mission_stage>=2: obj="Collect remaining ECHO fragments."
    mission.text="OBJECTIVE\n"+obj+"\n[I] inventory  [M] map"

func update_environment():
    var light=clamp(1.0-abs(world_time-13.0)/8.0,0.08,1.0)
    sun.light_energy=0.12+light
    env.ambient_light_energy=0.2+light*0.45
    env.background_color=Color(0.02,0.03,0.07).linear_interpolate(Color(0.17,0.27,0.4),light)

func failsafe():
    if player.global_transform.origin.y < -20:
        player.respawn(player_spawn); toast_msg("Player recovered to safe ground.")
    if car.global_transform.origin.y < -20:
        car.respawn(car_spawn)

func toast_msg(text):
    toast.text=text
    var timer=get_tree().create_timer(3.5)
    timer.connect("timeout",self,"clear_toast")

func clear_toast():
    toast.text=""

func save_game():
    var data={"money":money,"reputation":reputation,"echo_level":echo_level,"mission_stage":mission_stage,"day":day,"world_time":world_time,"weather":weather,"inventory":inventory,"collected":collected,"player_pos":[player.translation.x,player.translation.y,player.translation.z],"car_pos":[car.translation.x,car.translation.y,car.translation.z]}
    Save.save(data); Save.save_settings(settings); toast_msg("Game saved.")

func load_game():
    var d=Save.load()
    if d==null or typeof(d)!=TYPE_DICTIONARY: return
    money=int(d.get("money",money)); reputation=int(d.get("reputation",0)); echo_level=int(d.get("echo_level",0)); mission_stage=int(d.get("mission_stage",0)); day=int(d.get("day",1)); world_time=float(d.get("world_time",18.2)); weather=str(d.get("weather","CLEAR")); inventory=d.get("inventory",inventory); collected=d.get("collected",{})
    var pp=d.get("player_pos",[])
    if pp.size()==3: player.translation=Vector3(float(pp[0]),max(float(pp[1]),1.2),float(pp[2]))
    var cp=d.get("car_pos",[])
    if cp.size()==3: car.translation=Vector3(float(cp[0]),max(float(cp[1]),0.9),float(cp[2]))
    for n in get_children():
        if n is Area and n.has_meta("echo_id") and collected.has(str(int(n.get_meta("echo_id")))): n.visible=false

func load_settings():
    var d=Save.load_settings()
    if d!=null and typeof(d)==TYPE_DICTIONARY: settings=d

func apply_settings():
    if music: music.volume_db=linear2db(max(0.001,float(settings.get("music",0.65))))
    if ambient: ambient.volume_db=linear2db(max(0.001,float(settings.get("music",0.65))*0.65))
    if sfx: sfx.volume_db=linear2db(max(0.001,float(settings.get("sfx",0.85))))
    for b in touch_controls: b.visible=bool(settings.get("touch",true)) and game_started and not menu_root.visible
    var q=int(settings.get("quality",1))
    env.fog_enabled=q>0
    sun.shadow_enabled=q>0
