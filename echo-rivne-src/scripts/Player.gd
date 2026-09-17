extends KinematicBody
class_name EchoPlayer

var velocity = Vector3()
var walk_speed = 7.5
var sprint_speed = 12.0
var gravity = 22.0
var enabled = true
var touch_forward = false
var touch_back = false
var touch_left = false
var touch_right = false
var touch_sprint = false

func _physics_process(delta):
    if not enabled:
        return
    var dir = Vector3()
    if Input.is_action_pressed("move_forward") or touch_forward: dir.z -= 1.0
    if Input.is_action_pressed("move_back") or touch_back: dir.z += 1.0
    if Input.is_action_pressed("move_left") or touch_left: dir.x -= 1.0
    if Input.is_action_pressed("move_right") or touch_right: dir.x += 1.0
    dir = dir.normalized()
    var spd = sprint_speed if (Input.is_action_pressed("sprint") or touch_sprint) else walk_speed
    velocity.x = dir.x * spd
    velocity.z = dir.z * spd
    velocity.y = -0.5 if is_on_floor() else velocity.y - gravity * delta
    velocity = move_and_slide(velocity, Vector3.UP)

func set_touch(action, active):
    match action:
        "forward": touch_forward = active
        "back": touch_back = active
        "left": touch_left = active
        "right": touch_right = active
        "sprint": touch_sprint = active
