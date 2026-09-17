extends KinematicBody
class_name EchoPlayer

var velocity = Vector3()
var walk_speed = 7.5
var sprint_speed = 12.0
var gravity = 22.0
var enabled = true
var spawn_point = Vector3(0, 1.8, 28)

var touch_forward = false
var touch_back = false
var touch_left = false
var touch_right = false
var touch_sprint = false

var anim_time = 0.0
onready var arm_l = get_node_or_null("Visual/ArmL")
onready var arm_r = get_node_or_null("Visual/ArmR")
onready var leg_l = get_node_or_null("Visual/LegL")
onready var leg_r = get_node_or_null("Visual/LegR")
onready var head = get_node_or_null("Visual/Head")
onready var torso = get_node_or_null("Visual/Torso")

func _ready():
    spawn_point = global_transform.origin

func _physics_process(delta):
    if global_transform.origin.y < -20.0:
        respawn(spawn_point)

    if not enabled:
        _animate_body(delta, 0.0, false)
        return

    var dir = Vector3()
    if Input.is_action_pressed("move_forward") or touch_forward:
        dir.z -= 1.0
    if Input.is_action_pressed("move_back") or touch_back:
        dir.z += 1.0
    if Input.is_action_pressed("move_left") or touch_left:
        dir.x -= 1.0
    if Input.is_action_pressed("move_right") or touch_right:
        dir.x += 1.0

    dir = dir.normalized()
    var sprinting = Input.is_action_pressed("sprint") or touch_sprint
    var spd = sprint_speed if sprinting else walk_speed
    velocity.x = dir.x * spd
    velocity.z = dir.z * spd

    if dir.length() > 0.05:
        look_at(global_transform.origin + Vector3(dir.x, 0.0, dir.z), Vector3.UP)
        rotation.x = 0.0
        rotation.z = 0.0

    if is_on_floor():
        velocity.y = -0.2
    else:
        velocity.y -= gravity * delta

    var snap = Vector3.DOWN * 0.35
    if not is_on_floor() and velocity.y > 0.0:
        snap = Vector3.ZERO
    velocity = move_and_slide_with_snap(velocity, snap, Vector3.UP, true, 4, 0.785398, true)
    _animate_body(delta, dir.length(), sprinting)

func _animate_body(delta, move_amount, sprinting):
    if not torso:
        return
    anim_time += delta * (8.0 if sprinting else 5.5) * max(move_amount, 0.25)
    var swing = sin(anim_time) * 0.9 * move_amount
    var bounce = abs(sin(anim_time * 0.5)) * 0.08 * move_amount
    torso.translation.y = 1.0 + bounce
    if head:
        head.rotation.z = sin(anim_time * 0.5) * 0.04 * move_amount
    if arm_l:
        arm_l.rotation.x = swing
    if arm_r:
        arm_r.rotation.x = -swing
    if leg_l:
        leg_l.rotation.x = -swing
    if leg_r:
        leg_r.rotation.x = swing

func respawn(pos):
    velocity = Vector3.ZERO
    global_transform.origin = pos

func set_touch(action, active):
    match action:
        "forward": touch_forward = active
        "back": touch_back = active
        "left": touch_left = active
        "right": touch_right = active
        "sprint": touch_sprint = active
