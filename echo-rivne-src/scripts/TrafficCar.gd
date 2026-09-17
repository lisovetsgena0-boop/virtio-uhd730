extends Spatial
class_name EchoTrafficCar

var route = []
var speed = 7.0
var index = 0
var progress = 0.0

func setup(points, car_speed):
    route = points
    speed = car_speed
    if route.size() > 0:
        translation = route[0]

func _process(delta):
    if route.size() < 2:
        return
    var target = route[(index + 1) % route.size()]
    var pos = translation
    var dir = target - pos
    dir.y = 0.0
    var dist = dir.length()
    if dist < 0.45:
        index = (index + 1) % route.size()
        return
    dir = dir.normalized()
    translation += dir * min(speed * delta, dist)
    look_at(global_transform.origin + dir, Vector3.UP)
    rotation.x = 0.0
    rotation.z = 0.0
