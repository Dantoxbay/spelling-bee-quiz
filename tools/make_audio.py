"""Record every word in tools/words.txt with the Kokoro voice into app/src/main/assets/audio/<word>.ogg"""
import os, subprocess, sys, time
import numpy as np, soundfile as sf
from kokoro_onnx import Kokoro

VOICE = os.environ.get('VOICE', 'af_heart')   # clear American English voice
SPEED = float(os.environ.get('SPEED', '0.9'))
OUT = 'app/src/main/assets/audio'
os.makedirs(OUT, exist_ok=True)

words = [w.strip() for w in open('tools/words.txt') if w.strip()]
todo = [w for w in words if not os.path.exists(f'{OUT}/{w}.ogg')]
print(f'{len(words)} words, {len(todo)} to record with {VOICE} @ {SPEED}', flush=True)

k = Kokoro('kokoro-v1.0.onnx', 'voices-v1.0.bin')
t0 = time.time()
for n, w in enumerate(todo, 1):
    samples, sr = k.create(w + '.', voice=VOICE, speed=SPEED, lang='en-us')
    pad = np.zeros(int(sr * 0.08), dtype=np.float32)
    sf.write('/tmp/w.wav', np.concatenate([pad, samples.astype(np.float32), pad]), sr)
    subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', '/tmp/w.wav', '-ac', '1', '-ar', '24000',
                    '-c:a', 'libopus', '-b:a', '28k', '-application', 'voip', f'{OUT}/{w}.ogg'], check=True)
    if n % 200 == 0 or n == len(todo):
        print(f'{n}/{len(todo)}  {time.time() - t0:.0f}s', flush=True)
print('done')
