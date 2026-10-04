# Original art — CC0 1.0

All Walking Buddy art in `app/src/main/assets/`, the launcher foreground, the
Canvas icons in `IconView.java`, and `art/buddies.png` was created from scratch
for this project. No art was copied from Step Pals or another game.

To the extent possible under law, the contributors dedicate these art assets
to the public domain under [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/).
You may use, modify, and redistribute the art for any purpose, including
commercial use, without attribution. The generation code is MIT licensed.

## Asset inventory

| Asset | Description |
| --- | --- |
| `milo.png` | Tabby cat, four idle/blink frames |
| `maple.png` | Corgi, four idle/blink frames |
| `rusty.png` | Fox, four idle/blink frames |
| `mochi.png` | Bunny, four idle/blink frames |
| `fern.png` | Frog, four idle/blink frames |
| `bamboo.png` | Panda, four idle/blink frames |
| `pip.png` | Penguin, four idle/blink frames |
| `nova.png` | Dragon, four idle/blink frames |
| `meadow.png` | Green meadow and cottage |
| `sunset.png` | Warm walking landscape |
| `alpine.png` | Cool mountain landscape |
| `icon_foreground.png` | Adaptive launcher cat |
| `ic_notification.xml` | Native leaf notification icon |
| `IconView.java` | Native vector navigation and care icons |

Sprites are authored on a 64 × 64 pixel grid and exported as 256 × 256 frames
in 1024 × 256 PNG sheets. Backgrounds use a 192 × 128 grid, exported at 768 × 512.
Native nearest-neighbor rendering keeps the pixels crisp at phone resolutions.
Run `python3 tools/generate_art.py` with Pillow installed to regenerate everything.
