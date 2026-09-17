extends KinematicBody
class_name EchoCar

var speed = 0.0
var max_speed = 31.0
var reverse_speed = 10.0
var acceleration = 18.0
var braking = 26.0
var friction = 8.0
var steer_speed = 1.6
var occupied = false
var touch_forward = false
var touch_back = false
var touch_left = false
var touch_right = false

func _physics_process(delta):
    if not occupied:
        speed = move_toward(speed, 0.0, friction * delta)
        if abs(speed) > 0.1: move_and_slide(-global_transform.basis.z * speed, Vector3.UP)
        return
    var forward = Input.is_action_pressed("move_forward") or touch_forward
    var back = Input.is_action_pressed("move_back") or touch_back
    var left = Input.is_action_pressed("move_left") or touch_left
    var right = Input.is_action_pressed("move_right") or touch_right
    if forward: speed = move_toward(speed, max_speed, acceleration * delta)
    elif back: speed = move_toward(speed, -reverse_speed, braking * delta)
    else: speed = move_toward(speed, 0.0, friction * delta)
    var steer = 0.0
    if left: steer += 1.0
    if right: steer -= 1.0
    if abs(speed) > 0.4: rotation.y += steer * steer_speed * delta * clamp(abs(speed)/8.0,0.25,1.2) * sign(speed)
    move_and_slide(-global_transform.basis.z * speed, Vector3.UP)

func set_touch(action, active):
    match action:
        "forward": touch_forward = active
        "back": touch_back = active
        "left": touch_left = active
        "right": touch_right = active
