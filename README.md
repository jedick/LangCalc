# LangCalc

<img src="https://raw.githubusercontent.com/jedick/LangCalc/main/assets/langcalc-icon-outline.svg" alt="LangCalc icon" width="100">

A voice-driven calculator.
Say "what's 2 times 20" and it does the math on-device, with no internet connection needed.

LangCalc runs a small fine-tuned language model (FunctionGemma, 270M parameters)
that turns spoken requests into a calculator operation.
The model doesn't do arithmetic itself; it decides *what* to calculate,
and deterministic functions do the actual math.
That split keeps answers reliable even from a very small model.

## Try it now

There are two ways to try LangCalc.
The fastest way to see the model in action is to run the inference notebook (this launches the notebook with Colab's hosted runtime, not on-device):

<a target="_blank" href="https://colab.research.google.com/github/jedick/LangCalc/blob/main/model/notebooks/LangCalc-inference.ipynb">
<img src="https://www.tensorflow.org/images/colab_logo_32px.png" />Run LangCalc inference in Google Colab</a><br><br>

To run the model on your device, install the LangCalc custom task in [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery),
Google's app for running on-device AI models:

1. Install the AI Edge Gallery app.
2. Add the LangCalc custom task (see [`app/gallery-task/`](app/gallery-task/) for setup steps).
3. Open "Voice Calculator" from the home screen, pick a model, and start talking.

The LangCalc task integrates with the voice transcription service on your device and provides a chat-like calculator interface to show the results.

A standalone Android app is in progress; see [Roadmap](#roadmap) below.

## How it works

```
You:         "What is 2 times 20?"
Calculator:  2 * 20   <- shown on screen, editable if the model gets it wrong
Result:      40       <- computed locally
```

The model's job is to produce a function call like `multiply(2, 20)`.
Evaluating the function call happens with plain arithmetic code, not another model call.

## Fine-tuning and evaluation

For the fine-tuning and evaluation notebooks, see [`model/notebooks/`](model/notebooks/).

🌍 The LangCalc training and test data are multilingual, with both English (en) and Chinese (zh) prompts for the same calculator functions.

The base FunctionGemma model performs poorly on English prompts (spoken calculator requests), achieving only 20% correct function calls.
Fine-tuning with English prompts yields a large performance jump,
while fine-tuning with *both* English and Chinese prompts gives the best results for this small model.

<img src="https://chnosz.net/guest/LangCalc/langcalc-test-results_v02.png" alt="LangCalc test results" width="70%">

The strong performance of the `en.zh` model trained on two languages extends to the Chinese test data.

| Model | % Correct (en) | % Correct (zh) |
|-|-|-|
| jedick/functiongemma-langcalc-en.zh | 81 | 89 |
| jedick/functiongemma-langcalc-en | 63 | 85 |
| jedick/functiongemma-langcalc-zh | 38 | 84 |
| google/gemma-4-E2B-it | 100 | 100 |

Fine-tuning the small FunctionGemma model (270M) gets most of the way to what a much larger model (Gemma 4 E2B-it) can do at a fraction of the size, making it suitable for mobile deployment.

## Roadmap

- [x] Fine-tuned FunctionGemma model
- [x] Working demo via Google AI Edge Gallery
- [ ] Standalone Android app
- [ ] Multilingual support (beyond English)

## Repository layout

```
LangCalc/
├── app/
│   ├── gallery-task/     # plugin for Google AI Edge Gallery
│   └── android/          # standalone Android app [planned]
└── model/
    ├── notebooks/        # fine-tuning and inference notebooks (Colab)
    └── data/             # training and test data
```

## License

See [LICENSE](LICENSE).
