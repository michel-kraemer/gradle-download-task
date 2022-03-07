import fs from "fs"
import nunjucks from "nunjucks"
import { optimize } from "svgo"
import prettyBytes from "pretty-bytes"
import subsetFont from "subset-font"

let charWidth = 9.7
let keyframes = []
let line1 = "gradle downloadFile"
let line1Ids = [50, 120, 200, 240, 250, 300, 370, 480, 590, 600, 710, 820, 930, 1140, 1250, 1360, 1470, 1580, 1600]
let id = 0

let seconds = [0, 1, 2, 3, 4]

let bytes = ["0 KB"]
let currentBytes = 0
let totalBytes = 187.78 * 1000 * 1000
let bytesSteps = 40
for (let i = 1; i <= bytesSteps; ++i) {
  currentBytes = totalBytes / bytesSteps * i
  bytes.push(prettyBytes(currentBytes, { minimumFractionDigits: 2, maximumFractionDigits: 2 }))
}

// initialize everything
function clearEverything() {
  for (let i = 0; i < line1.length; ++i) {
    keyframes.push({
      id,
      ["line1_" + i]: { display: "none" }
    })
  }
  keyframes.push({
    id,
    line1cursor: { display: "inline" },
    line2cursor: { display: "none" },
    line3: { display: "none" },
    line4: { display: "none" },
    line5: { display: "none" },
    line6: { display: "none" },
    line7: { display: "none" },
    line8: { display: "none" },
    downloadcursor: { display: "none" },
    prelastlinecursor: { display: "none" },
    lastline: { display: "none" },
    lastlinecursor: { display: "none" }
  })
  for (let i = 0; i < seconds.length; ++i) {
    keyframes.push({ id, ["seconds_" + i]: { display: "none" } })
  }
  for (let i = 0; i < bytes.length; ++i) {
    keyframes.push({ id, ["bytes_" + i]: { display: "none" } })
  }
}
clearEverything()

// type first line
for (let i = 0; i < line1.length; ++i) {
  id = line1Ids[i]
  keyframes.push({
    id,
    ["line1_" + i]: { display: "inline" }
  })
}

// hide first line cursor - let second line cursor blink
id += 100
keyframes.push({
  id,
  line1cursor: { display: "none" },
  line2cursor: { display: "inline" }
})
id += 500
keyframes.push({
  id,
  line2cursor: { display: "none" }
})
id += 500
keyframes.push({
  id,
  line2cursor: { display: "inline" }
})

// display line 3, 4, 5, and 6
id += 100
keyframes.push({
  id,
  line2cursor: { display: "none" },
  line3: { display: "inline" },
  line4: { display: "inline" },
  line5: { display: "inline" },
  line6: { display: "inline" },
  seconds_0: { display: "inline" },
  bytes_0: { display: "inline" },
  downloadcursor: { display: "inline" }
})

// render elapsed time
for (let i = 1; i < seconds.length; ++i) {
  keyframes.push({
    id: id + (i * 1000),
    ["seconds_" + (i - 1)]: { display: "none" },
    ["seconds_" + i]: { display: "inline" }
  })
}

// render downloaded bytes
for (let i = 1; i < bytes.length; ++i) {
  let duration = seconds[seconds.length - 1] + 1
  let elapsed = duration / bytes.length * i
  keyframes.push({
    id: id + elapsed * 1000,
    ["bytes_" + (i - 1)]: { display: "none" },
    ["bytes_" + i]: { display: "inline" }
  })
}

// let download cursor blink
for (let i = 0; i < seconds.length; ++i) {
  keyframes.push({
    id: id + (i * 1000 + 500),
    downloadcursor: { display: "none" }
  })
  keyframes.push({
    id: id + (i * 1000 + 1000),
    downloadcursor: { display: "inline" }
  })
}

id += seconds.length * 1000

// wait a little bit
id += 200

// hide line 5 and 6 but display 7 and 8 and pre-last-line cursor instead
keyframes.push({
  id,
  line5: { display: "none" },
  line6: { display: "none" },
  line7: { display: "inline" },
  line8: { display: "inline" },
  downloadcursor: { display: "none" },
  prelastlinecursor: { display: "inline" }
})

// wait a little bit
id += 500

// replace pre-last-line cursor with lastline
keyframes.push({
  id,
  prelastlinecursor: { display: "none" },
  lastline: { display: "inline" },
  lastlinecursor: { display: "inline" }
})

// let last-line cursor blink
id += 500
keyframes.push({
  id,
  lastlinecursor: { display: "none" }
})
id += 500
keyframes.push({
  id,
  lastlinecursor: { display: "inline" }
})
id += 500
keyframes.push({
  id,
  lastlinecursor: { display: "none" }
})
id += 500
keyframes.push({
  id,
  lastlinecursor: { display: "inline" }
})
id += 500
keyframes.push({
  id,
  lastlinecursor: { display: "none" }
})

// finalize everything
clearEverything()

// convert keyframe timings to percent
for (let k of keyframes) {
  k.id = +(k.id / id).toFixed(5)
}

// convert keyframes to animations
let animations = {}
for (let k of keyframes) {
  for (let key of Object.keys(k)) {
    if (key === "id") {
      continue
    }

    let a = animations[key]
    if (a === undefined) {
      a = []
      animations[key] = a
    }

    let value = k[key]
    for (let attributeName of Object.keys(value)) {
      let c = a.find(e => e.attributeName === attributeName)
      if (c === undefined) {
        c = {
          attributeName,
          values: [],
          keyTimes: []
        }
        a.push(c)
      }
      c.values.push(value[attributeName])
      c.keyTimes.push(k.id)
    }
  }
}

// load and convert fonts
let dejaVuMono400 = fs.readFileSync("node_modules/@fontsource/dejavu-mono/files/dejavu-mono-latin-400-normal.woff2")
let dejaVuMono700 = fs.readFileSync("node_modules/@fontsource/dejavu-mono/files/dejavu-mono-latin-700-normal.woff2")
let printableCharacters = ""
for (let i = 32; i < 127; ++i) {
  printableCharacters += String.fromCharCode(i)
}
let dejaVuMono400Subset = await subsetFont(dejaVuMono400, printableCharacters, { targetFormat: "woff2" })
let dejaVuMono700Subset = await subsetFont(dejaVuMono700, printableCharacters, { targetFormat: "woff2" })
dejaVuMono400Subset = dejaVuMono400Subset.toString("base64")
dejaVuMono700Subset = dejaVuMono700Subset.toString("base64")

let env = nunjucks.configure({ autoescape: false })
let res = env.render("screencast.svg.template", {
  totalTime: id,
  charWidth,
  animations,
  line1,
  seconds,
  bytes,
  dejaVuMono400Subset,
  dejaVuMono700Subset
})
res = optimize(res, {
  multipass: true,
  plugins: [{
    name: "preset-default",
    params: {
      overrides: {
        removeHiddenElems: false
      }
    }
  }]
})
fs.writeFileSync("screencast.svg", res.data, { encoding: "utf-8" })
