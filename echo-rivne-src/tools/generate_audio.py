import os, math, wave, struct, random
BASE=os.path.join(os.path.dirname(os.path.dirname(__file__)), 'assets', 'audio')
os.makedirs(BASE, exist_ok=True)
SR=22050
random.seed(1337)

def write_wav(name, samples):
    path=os.path.join(BASE,name)
    with wave.open(path,'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        frames=bytearray()
        for s in samples:
            s=max(-1.0,min(1.0,s))
            frames += struct.pack('<h', int(s*32767))
        w.writeframes(frames)

def adsr(t,dur,a=0.02,d=0.1,s=0.7,r=0.2):
    if t<0 or t>dur: return 0.0
    if t<a: return t/max(a,1e-6)
    if t<a+d: return 1-(1-s)*((t-a)/max(d,1e-6))
    if t<dur-r: return s
    return s*(1-(t-(dur-r))/max(r,1e-6))

notes=[220.0,246.94,293.66,329.63,392.0,329.63,293.66,246.94]
length=18.0; samples=[]
for i in range(int(SR*length)):
    t=i/SR; beat=t*2.0; idx=int(beat)%len(notes); n=notes[idx]
    local=t-(int(beat)/2.0); env=adsr(local,0.5,0.02,0.12,0.45,0.18)
    s=0.24*env*math.sin(2*math.pi*n*t)+0.14*env*math.sin(2*math.pi*(n/2.0)*t+0.3)
    s+=0.05*math.sin(2*math.pi*0.11*t)+0.02*random.uniform(-1,1)
    samples.append(s*0.8)
write_wav('menu_theme.wav',samples)

length=12.0; samples=[]; prev=0.0
for i in range(int(SR*length)):
    t=i/SR
    s=0.18*math.sin(2*math.pi*110*t)+0.08*math.sin(2*math.pi*55*t+1.7)+0.03*math.sin(2*math.pi*0.07*t)
    prev=0.96*prev+0.04*random.uniform(-1,1); s+=0.05*prev
    pulse=((math.sin(2*math.pi*1.5*t)+1)/2)**8
    s+=0.08*pulse*math.sin(2*math.pi*330*t)
    samples.append(s*0.55)
write_wav('city_loop.wav',samples)

length=0.55; samples=[]
for i in range(int(SR*length)):
    t=i/SR; f=540+720*t; env=adsr(t,length,0.005,0.06,0.55,0.20)
    samples.append(0.55*env*math.sin(2*math.pi*f*t)+0.20*env*math.sin(2*math.pi*(f*1.5)*t))
write_wav('pickup.wav',samples)

length=0.18; samples=[]
for i in range(int(SR*length)):
    t=i/SR; env=adsr(t,length,0.001,0.01,0.3,0.05)
    samples.append(0.6*env*math.sin(2*math.pi*(900-300*t)*t)+0.12*env*random.uniform(-1,1))
write_wav('click.wav',samples)
print('generated audio:', BASE)
