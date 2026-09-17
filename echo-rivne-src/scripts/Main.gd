extends Spatial

const PlayerScript = preload("res://scripts/Player.gd")
const CarScript = preload("res://scripts/Car.gd")
const NPCScript = preload("res://scripts/NPC.gd")
const Save = preload("res://scripts/SaveSystem.gd")

var player
var car
var active_actor
var in_car = false
var money = 1200
var reputation = 0
var echo_level = 0
var mission_stage = 0
var world_time = 18.25
var day = 1
var phone_open = false
var collected = {}
var hud
var mission
var hint
var phone_panel
var phone_text
var toast
var env
var sun

var locations = {
    "GARAGE": Vector3(38,0,48),
    "ECHO": Vector3(-155,0,-177)
}

func _ready():
    randomize()
    setup_environment()
    build_city()
    spawn_player()
    spawn_car()
    spawn_npcs()
    build_ui()
    load_game()
    show_toast("РІВНЕ. Перевір телефон.")

func _process(delta):
    if Input.is_action_just_pressed("phone"): toggle_phone()
    if Input.is_action_just_pressed("vehicle"): toggle_vehicle()
    if Input.is_action_just_pressed("interact"): interact()
    if Input.is_action_just_pressed("save_game"): save_game()
    world_time += delta * 0.012
    if world_time >= 24.0:
        world_time -= 24.0
        day += 1
    update_environment()
    update_interaction()
    update_hud()

func make_mat(color, emission=Color(0,0,0)):
    var m = SpatialMaterial.new()
    m.albedo_color = color
    m.roughness = 0.82
    if emission != Color(0,0,0):
        m.emission_enabled = true
        m.emission = emission
        m.emission_energy = 1.5
    return m

func box(parent, name, pos, size, color, collision=true, emission=Color(0,0,0)):
    var body = StaticBody.new() if collision else Spatial.new()
    body.name = name
    body.translation = pos
    parent.add_child(body)
    var mesh = MeshInstance.new()
    var cube = CubeMesh.new()
    cube.size = size
    mesh.mesh = cube
    mesh.material_override = make_mat(color, emission)
    body.add_child(mesh)
    if collision:
        var cs = CollisionShape.new()
        var shape = BoxShape.new()
        shape.extents = size * 0.5
        cs.shape = shape
        body.add_child(cs)
    return body

func setup_environment():
    var wn = WorldEnvironment.new()
    env = Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.12,0.17,0.24)
    env.ambient_light_enabled = true
    env.ambient_light_color = Color(0.55,0.62,0.74)
    env.ambient_light_energy = 0.6
    env.fog_enabled = true
    env.fog_color = Color(0.24,0.28,0.34)
    env.fog_depth_begin = 100
    env.fog_depth_end = 330
    wn.environment = env
    add_child(wn)
    sun = DirectionalLight.new()
    sun.rotation_degrees = Vector3(-45,-35,0)
    sun.light_energy = 1.0
    sun.shadow_enabled = true
    add_child(sun)
    box(self,"Ground",Vector3(0,-0.65,0),Vector3(470,1,470),Color(0.14,0.19,0.15),true)

func road(pos, size):
    box(self,"Road",pos,size,Color(0.055,0.06,0.065),true)
    if size.x > size.z:
        for x in range(int(pos.x-size.x/2)+8,int(pos.x+size.x/2)-8,16):
            box(self,"Mark",Vector3(x,pos.y+0.13,pos.z),Vector3(7,0.03,0.25),Color(0.75,0.75,0.70),false)
    else:
        for z in range(int(pos.z-size.z/2)+8,int(pos.z+size.z/2)-8,16):
            box(self,"Mark",Vector3(pos.x,pos.y+0.13,z),Vector3(0.25,0.03,7),Color(0.75,0.75,0.70),false)

func building(pos, size, color, lit=false):
    box(self,"Building",pos+Vector3(0,size.y/2,0),size,color,true)
    var rows = int(clamp(size.y/5.0,1,6))
    var cols = int(clamp(size.x/7.0,1,7))
    for y in range(rows):
        for x in range(cols):
            if (x+y)%2 == 0:
                var wx = pos.x-size.x/2+3+x*(size.x/max(cols,1))
                box(self,"Window",Vector3(wx,3+y*4.5,pos.z-size.z/2-0.04),Vector3(2,1.5,0.08),Color(0.20,0.23,0.27),false,Color(0.95,0.60,0.20) if lit else Color(0,0,0))

func tree(pos):
    box(self,"Trunk",pos+Vector3(0,2,0),Vector3(0.5,4,0.5),Color(0.22,0.12,0.07),false)
    var mesh = MeshInstance.new()
    var sphere = SphereMesh.new()
    sphere.radius = 2.1
    sphere.height = 4.2
    mesh.mesh = sphere
    mesh.translation = pos+Vector3(0,5,0)
    mesh.material_override = make_mat(Color(0.09,0.31,0.14))
    add_child(mesh)

func build_city():
    road(Vector3(0,0,0),Vector3(400,0.22,20))
    road(Vector3(0,0,76),Vector3(330,0.22,14))
    road(Vector3(0,0,-82),Vector3(330,0.22,14))
    road(Vector3(-100,0,0),Vector3(14,0.22,330))
    road(Vector3(100,0,0),Vector3(14,0.22,330))
    road(Vector3(-175,0,0),Vector3(12,0.22,260))
    road(Vector3(175,0,-40),Vector3(12,0.22,250))

    # Театральна площа / драмтеатр
    box(self,"Teatralna",Vector3(-8,0.05,-27),Vector3(78,0.12,42),Color(0.32,0.33,0.32),false)
    building(Vector3(-8,0,-58),Vector3(48,17,20),Color(0.66,0.60,0.49),true)
    for x in [-20,-12,-4,4,12]:
        box(self,"Column",Vector3(x,9,-47.7),Vector3(2,11,2),Color(0.84,0.79,0.68),false)
    box(self,"TheatreSign",Vector3(-8,16.5,-47.5),Vector3(28,1.2,0.3),Color(0.75,0.2,0.13),false,Color(0.85,0.12,0.08))

    var blocks = [
        [Vector3(-152,0,-36),Vector3(38,18,34),Color(0.46,0.44,0.41)],
        [Vector3(-122,0,36),Vector3(44,14,34),Color(0.54,0.50,0.44)],
        [Vector3(-65,0,-38),Vector3(38,15,34),Color(0.58,0.54,0.47)],
        [Vector3(-60,0,37),Vector3(48,12,32),Color(0.46,0.48,0.47)],
        [Vector3(48,0,-38),Vector3(54,22,34),Color(0.39,0.42,0.47)],
        [Vector3(54,0,37),Vector3(52,17,34),Color(0.51,0.51,0.50)],
        [Vector3(135,0,-38),Vector3(52,16,34),Color(0.60,0.53,0.46)],
        [Vector3(144,0,37),Vector3(46,13,34),Color(0.49,0.50,0.48)]
    ]
    for b in blocks: building(b[0],b[1],b[2],true)

    # Лебединка
    box(self,"LebedynkaPark",Vector3(72,0.02,112),Vector3(115,0.08,58),Color(0.10,0.26,0.13),false)
    box(self,"LebedynkaWater",Vector3(72,0.07,112),Vector3(52,0.05,28),Color(0.055,0.21,0.30),false)
    for i in range(20): tree(Vector3(18+(i*23)%108,0,87+(i*31)%50))

    # Басів Кут
    box(self,"BasivKutLake",Vector3(-87,0.03,173),Vector3(180,0.05,64),Color(0.05,0.19,0.28),false)
    for i in range(16): building(Vector3(-168+(i%8)*24,0,129+(i/8)*27),Vector3(15,5+(i%3),13),Color(0.49,0.42,0.35),false)

    # Північний
    for i in range(12): building(Vector3(120+(i%4)*27,0,-172+(i/4)*42),Vector3(20,25+(i%3)*7,31),Color(0.40,0.42,0.46),i%4==0)

    # гаражі / ринок
    for i in range(8): building(Vector3(-30+(i%4)*22,0,66+(i/4)*20),Vector3(17,5,14),Color(0.32,0.34,0.33),i%3==0)
    box(self,"GarageMarker",locations["GARAGE"]+Vector3(0,2.2,0),Vector3(3,4,3),Color(0.14,0.55,0.83),false,Color(0.05,0.4,1))

    # промзона / ECHO
    building(Vector3(-155,0,-150),Vector3(78,12,48),Color(0.24,0.27,0.28),false)
    building(Vector3(-195,0,-108),Vector3(42,9,62),Color(0.29,0.29,0.27),false)
    box(self,"EchoTower",Vector3(-155,24,-150),Vector3(14,36,14),Color(0.08,0.10,0.12),true)
    box(self,"EchoCore",locations["ECHO"]+Vector3(0,1.5,0),Vector3(5,3,5),Color(0.04,0.18,0.19),false,Color(0.0,0.95,0.85))

    var frags = [Vector3(-8,1,-25),Vector3(72,1,112),Vector3(-110,1,145),Vector3(150,1,-150),Vector3(-155,1,-178),Vector3(38,1,48)]
    for i in range(frags.size()): spawn_fragment(i,frags[i])

func spawn_fragment(id,pos):
    var a = Area.new()
    a.name = "EchoFragment_%d" % id
    a.translation = pos
    a.set_meta("echo_id",id)
    a.set_meta("kind","echo")
    add_child(a)
    var cs = CollisionShape.new()
    var sh = SphereShape.new()
    sh.radius = 1.6
    cs.shape = sh
    a.add_child(cs)
    var mesh = MeshInstance.new()
    var sp = SphereMesh.new()
    sp.radius = 0.55
    sp.height = 1.1
    mesh.mesh = sp
    mesh.material_override = make_mat(Color(0.03,0.35,0.38),Color(0,1,0.85))
    a.add_child(mesh)

func spawn_player():
    player = KinematicBody.new()
    player.name = "Player"
    player.script = PlayerScript
    player.translation = Vector3(0,1.4,28)
    add_child(player)
    var cs = CollisionShape.new()
    var cap = CapsuleShape.new()
    cap.radius = 0.52
    cap.height = 1.05
    cs.shape = cap
    player.add_child(cs)
    var mesh = MeshInstance.new()
    var cm = CapsuleMesh.new()
    cm.radius = 0.52
    cm.mid_height = 1.05
    mesh.mesh = cm
    mesh.material_override = make_mat(Color(0.13,0.62,0.86))
    player.add_child(mesh)
    var cam = Camera.new()
    cam.name = "Camera"
    cam.translation = Vector3(0,7.5,10.5)
    cam.rotation_degrees = Vector3(-24,0,0)
    cam.current = true
    player.add_child(cam)
    active_actor = player

func spawn_car():
    car = KinematicBody.new()
    car.name = "Car"
    car.script = CarScript
    car.translation = Vector3(24,0.7,22)
    add_child(car)
    var cs = CollisionShape.new()
    var sh = BoxShape.new()
    sh.extents = Vector3(1.15,0.6,2.25)
    cs.shape = sh
    car.add_child(cs)
    box(car,"Body",Vector3(0,0.55,0),Vector3(2.3,0.85,4.5),Color(0.11,0.24,0.40),false)
    box(car,"Cabin",Vector3(0,1.25,-0.1),Vector3(1.85,0.72,2.3),Color(0.08,0.11,0.15),false)
    var cam = Camera.new()
    cam.name = "Camera"
    cam.translation = Vector3(0,5.3,10)
    cam.rotation_degrees = Vector3(-18,0,0)
    car.add_child(cam)

func spawn_npcs():
    var centers = [Vector3(0,0,25),Vector3(70,0,92),Vector3(135,0,-95),Vector3(-100,0,40)]
    for i in range(24):
        var n = KinematicBody.new()
        n.name = "NPC_%02d" % i
        n.script = NPCScript
        var c = centers[i%centers.size()]
        n.translation = c+Vector3((i*7)%18-9,1.1,(i*11)%18-9)
        add_child(n)
        n.setup(c,6.0+(i%5)*2.0,float(i))
        var cs = CollisionShape.new()
        var cap = CapsuleShape.new()
        cap.radius = 0.42
        cap.height = 0.9
        cs.shape = cap
        n.add_child(cs)
        var mesh = MeshInstance.new()
        var cm = CapsuleMesh.new()
        cm.radius = 0.42
        cm.mid_height = 0.9
        mesh.mesh = cm
        mesh.material_override = make_mat(Color(0.25+0.1*(i%3),0.38+0.05*(i%4),0.48))
        n.add_child(mesh)

func update_environment():
    var daylight = clamp(1.0-abs(world_time-13.0)/8.0,0.08,1.0)
    sun.light_energy = 0.12+daylight
    sun.rotation_degrees.x = -10.0-daylight*55.0
    env.ambient_light_energy = 0.20+daylight*0.48
    env.background_color = Color(0.025,0.035,0.07).linear_interpolate(Color(0.18,0.28,0.42),daylight)

func distance_to(pos):
    return active_actor.global_transform.origin.distance_to(pos) if active_actor else 9999.0

func update_interaction():
    var text = ""
    if not in_car and player.global_transform.origin.distance_to(car.global_transform.origin)<4.0: text = "[CAR] сісти в авто"
    if distance_to(locations["GARAGE"])<5.0: text = "[E] підробіток у гаражі +₴350"
    if distance_to(locations["ECHO"])<6.0: text = "[E] термінал ECHO"
    hint.text = text

func interact():
    if distance_to(locations["GARAGE"])<5.0:
        money += 350
        reputation += 1
        show_toast("Підробіток: +₴350")
        return
    if distance_to(locations["ECHO"])<6.0:
        mission_stage = max(mission_stage,2)
        echo_level += 2
        show_toast("ECHO: модель зафіксувала аномалію")
        return
    for n in get_children():
        if n is Area and n.has_meta("kind") and n.get_meta("kind")=="echo":
            var id = str(int(n.get_meta("echo_id")))
            if not collected.has(id) and player.global_transform.origin.distance_to(n.global_transform.origin)<3.0:
                collected[id] = true
                n.visible = false
                echo_level += 1
                reputation += 1
                if mission_stage==0: mission_stage=1
                show_toast("Фрагмент ECHO знайдено")
                return

func toggle_vehicle():
    if not in_car:
        if player.global_transform.origin.distance_to(car.global_transform.origin)>4.0:
            show_toast("Підійди до авто")
            return
        in_car = true
        player.enabled = false
        player.visible = false
        player.set_physics_process(false)
        car.occupied = true
        car.get_node("Camera").current = true
        active_actor = car
    else:
        in_car = false
        car.occupied = false
        player.global_transform.origin = car.global_transform.origin+car.global_transform.basis.x*2.4+Vector3(0,1,0)
        player.visible = true
        player.enabled = true
        player.set_physics_process(true)
        player.get_node("Camera").current = true
        active_actor = player

func toggle_phone():
    phone_open = not phone_open
    phone_panel.visible = phone_open
    phone_text.text = phone_content()

func phone_content():
    var story = "UNKNOWN: Знайди бірюзовий сигнал у центрі."
    if mission_stage==1: story = "UNKNOWN: Не шукай мене. Шукай місця, яких немає на карті."
    elif mission_stage>=2: story = "ECHO: Ти прийшов раніше, ніж передбачала модель."
    return "ECHO//PHONE\n\nБаланс: ₴%d\nРепутація: %d\nECHO: %d\nДень: %d  %02d:%02d\n\n%s\n\nМАРКЕТ\nБензин ₴58/л\nОренда ₴4200\nАвто ₴54000" % [money,reputation,echo_level,day,int(world_time),int((world_time-int(world_time))*60),story]

func build_ui():
    var layer = CanvasLayer.new()
    add_child(layer)
    hud = Label.new()
    hud.rect_position = Vector2(16,14)
    hud.rect_size = Vector2(700,70)
    layer.add_child(hud)
    mission = Label.new()
    mission.anchor_left = 1
    mission.anchor_right = 1
    mission.rect_position = Vector2(-390,18)
    mission.rect_size = Vector2(370,90)
    mission.align = Label.ALIGN_RIGHT
    layer.add_child(mission)
    hint = Label.new()
    hint.anchor_left = 0.5
    hint.anchor_right = 0.5
    hint.anchor_top = 1
    hint.anchor_bottom = 1
    hint.rect_position = Vector2(-220,-85)
    hint.rect_size = Vector2(440,40)
    hint.align = Label.ALIGN_CENTER
    layer.add_child(hint)
    toast = Label.new()
    toast.anchor_left = 0.5
    toast.anchor_right = 0.5
    toast.rect_position = Vector2(-300,105)
    toast.rect_size = Vector2(600,40)
    toast.align = Label.ALIGN_CENTER
    layer.add_child(toast)
    phone_panel = ColorRect.new()
    phone_panel.color = Color(0.02,0.04,0.06,0.96)
    phone_panel.anchor_left = 0.5
    phone_panel.anchor_right = 0.5
    phone_panel.anchor_top = 0.5
    phone_panel.anchor_bottom = 0.5
    phone_panel.rect_position = Vector2(-210,-285)
    phone_panel.rect_size = Vector2(420,570)
    phone_panel.visible = false
    layer.add_child(phone_panel)
    phone_text = Label.new()
    phone_text.rect_position = Vector2(22,22)
    phone_text.rect_size = Vector2(376,520)
    phone_text.autowrap = true
    phone_panel.add_child(phone_text)
    var vp = get_viewport().size
    layer.add_child(make_button("↑",Vector2(95,vp.y-180),Vector2(72,72),"forward"))
    layer.add_child(make_button("↓",Vector2(95,vp.y-100),Vector2(72,72),"back"))
    layer.add_child(make_button("←",Vector2(18,vp.y-100),Vector2(72,72),"left"))
    layer.add_child(make_button("→",Vector2(172,vp.y-100),Vector2(72,72),"right"))
    layer.add_child(action_button("E",Vector2(vp.x-230,vp.y-102),Vector2(72,72),"mobile_interact"))
    layer.add_child(action_button("CAR",Vector2(vp.x-150,vp.y-102),Vector2(72,72),"mobile_vehicle"))
    layer.add_child(action_button("☎",Vector2(vp.x-70,vp.y-102),Vector2(58,72),"mobile_phone"))

func make_button(text,pos,size,action):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("button_down",self,"touch_down",[action])
    b.connect("button_up",self,"touch_up",[action])
    return b

func action_button(text,pos,size,method):
    var b = Button.new()
    b.text = text
    b.rect_position = pos
    b.rect_size = size
    b.connect("pressed",self,method)
    return b

func touch_down(action):
    if in_car: car.set_touch(action,true)
    else: player.set_touch(action,true)

func touch_up(action):
    if in_car: car.set_touch(action,false)
    else: player.set_touch(action,false)

func mobile_interact(): interact()
func mobile_vehicle(): toggle_vehicle()
func mobile_phone(): toggle_phone()

func update_hud():
    var mode = "AUTO" if in_car else "ПІШКИ"
    hud.text = "ECHO//RIVNE  ₴%d  REP %d  ECHO %d\n%s • День %d • %02d:%02d" % [money,reputation,echo_level,mode,day,int(world_time),int((world_time-int(world_time))*60)]
    var obj = "Знайди перший сигнал ECHO у центрі"
    if mission_stage==1: obj = "Досліди промзону на заході"
    elif mission_stage>=2: obj = "Знайди решту фрагментів ECHO"
    mission.text = "ЗАВДАННЯ\n"+obj+"\n[P] телефон  [F5] save"

func show_toast(text):
    toast.text = text
    var t = get_tree().create_timer(3.0)
    t.connect("timeout",self,"clear_toast")

func clear_toast(): toast.text = ""

func save_game():
    Save.save({"money":money,"reputation":reputation,"echo_level":echo_level,"mission_stage":mission_stage,"world_time":world_time,"day":day,"collected":collected,"pp":[player.translation.x,player.translation.y,player.translation.z],"cp":[car.translation.x,car.translation.y,car.translation.z]})
    show_toast("Гру збережено")

func load_game():
    var d = Save.load()
    if d == null or typeof(d) != TYPE_DICTIONARY: return
    money = int(d.get("money",money))
    reputation = int(d.get("reputation",reputation))
    echo_level = int(d.get("echo_level",echo_level))
    mission_stage = int(d.get("mission_stage",mission_stage))
    world_time = float(d.get("world_time",world_time))
    day = int(d.get("day",day))
    collected = d.get("collected",{})
    var pp = d.get("pp",[])
    if pp.size()==3: player.translation = Vector3(float(pp[0]),float(pp[1]),float(pp[2]))
    var cp = d.get("cp",[])
    if cp.size()==3: car.translation = Vector3(float(cp[0]),float(cp[1]),float(cp[2]))
    for n in get_children():
        if n is Area and n.has_meta("echo_id") and collected.has(str(int(n.get_meta("echo_id")))): n.visible = false
