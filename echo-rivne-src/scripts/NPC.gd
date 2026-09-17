extends KinematicBody
class_name EchoNPC

var center = Vector3()
var radius = 8.0
var phase = 0.0
var speed = 1.3
var alive_time = 0.0

func setup(c, r, p):
    center = c
    radius = r
    phase = p

func _physics_process(delta):
    alive_time += delta
    var a = phase + alive_time * speed / max(radius, 1.0)
    var target = center + Vector3(cos(a)*radius,0,sin(a)*radius)
    var dir = target - global_transform.origin
    dir.y = 0
    if dir.length() > 0.2:
        dir = dir.normalized()
        move_and_slide(dir*speed, Vector3.UP)
        look_at(global_transform.origin + dir, Vector3.UP)
