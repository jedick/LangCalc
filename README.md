# LangCalc

<img src="https://raw.githubusercontent.com/jedick/LangCalc/main/assets/langcalc-icon-outline.svg" alt="LangCalc icon" width="100"/>

A voice-driven calculator.
Say "what's 2 times 20" and it does the math on-device, with no internet connection needed.

LangCalc runs a small fine-tuned language model (FunctionGemma, 270M parameters)
that turns spoken requests into a calculator operation.
The model doesn't do arithmetic itself; it decides *what* to calculate,
and deterministic functions do the actual math.
That split keeps answers reliable even from a very small model.

## Try it now

There are two ways to try LangCalc.
The fastest way to see the model in action is to run the inference notebook.
This launches the notebook with Colab's hosted runtime, not on-device:

<a target="_blank" href="https://colab.research.google.com/github/jedick/LangCalc/blob/main/model/notebooks/LangCalc-inference.ipynb">
<img src="https://www.tensorflow.org/images/colab_logo_32px.png" />&npsb;Run LangCalc inference in Google Colab</a><br><br>

To run the model on your device, install the LangCalc custom task in [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery),
Google's app for running on-device AI models.
The LangCalc task integrates with the voice transcription service on your device and provides a chat-like calculator interface to show the results.

<a href="app/langcalc/">
<img src="https://raw.githubusercontent.com/jedick/LangCalc/main/assets/ai-edge-gallery-icon.png" width=32px />&nbsp;LangCalc gallery task setup instructions</a><br><br>

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
Evaluations were performed on 100 test prompts (spoken calculator requests) for each language.

The base FunctionGemma model performs poorly on English test prompts, achieving only 23% correct function calls.
Fine-tuning with English examples yields a large performance jump,
and adding Chinese examples into the mix doesn't degrade the model's performance on English test data.

<img src="https://chnosz.net/guest/LangCalc/langcalc-test-results_v03.png" alt="LangCalc test results" width="70%">

The strong performance of the `en.zh` model trained on two languages extends to the Chinese test data.

| Model | % Correct (en) | % Correct (zh) |
|-|-|-|
| jedick/functiongemma-langcalc-en.zh | 79 | 87 |
| jedick/functiongemma-langcalc-en | 78 | 59 |
| jedick/functiongemma-langcalc-zh | 46 | 89 |
| google/gemma-4-E2B-it | 95 | 92 |

Our tests show that a single fine-tuned model can power a voice calculator with different spoken languages,
and the small fine-tuned model (FunctionGemma 270M) gets most of the way to what a much larger model (Gemma 4 E2B-it) can do.
The size savings and multilingual capabilities make this model suitable for mobile deployment.

## Roadmap

- [x] Fine-tuned FunctionGemma model
- [x] Working demo via Google AI Edge Gallery
- [ ] Standalone Android app
- [ ] Multilingual support (beyond English)

## Repository layout

```
LangCalc/
├── app/
│   ├── langcalc/         # plugin for Google AI Edge Gallery
│   └── android/          # standalone Android app [planned]
└── model/
    ├── data/             # training and test data
    ├── notebooks/        # fine-tuning, inference, and evaluation notebooks
    └── scripts/          # model format conversion scripts
```

## License

See [LICENSE](LICENSE).
