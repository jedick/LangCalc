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
<img src="https://www.tensorflow.org/images/colab_logo_32px.png" />&nbsp;Run LangCalc inference in Google Colab</a><br><br>

To run the model on your device, install the LangCalc custom task in [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery),
Google's app for running on-device AI models.
The LangCalc task integrates with the voice transcription service on your device and provides a chat-like calculator interface to show the results.

<a href="gallery-task/">
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

Fine-tuning and evaluation notebooks are available [in this repo](model/notebooks/).
Datasets and fine-tuned models are available on the [Hugging Face Hub](https://huggingface.co/jedick).

🌍 LangCalc training and test data are multilingual, with both English (en) and Chinese (zh) prompts for the same calculator functions.
Evaluations were performed on 120 test prompts (spoken calculator requests) for each language,
covering scenarios such as whole numbers, decimals, currency, comparison idioms, and percentages.

The base FunctionGemma model performs poorly on English test prompts, achieving only 21% correct function calls.
Fine-tuning with English examples yields a large performance jump,
and adding Chinese examples into the mix only slightly decreases the model's performance on English test data.

<img src="https://chnosz.net/guest/LangCalc/langcalc-test-results_v05.png" alt="LangCalc test results" width="80%">

A separate test split includes prompts with general knowledge, small talk, missing operands,
and other requests that should be declined as they are not calculator requests.
The fine-tuned model correctly declines 99% of these requests.
In contrast, Gemma 4 E2B-it (not fine-tuned) incorrectly calls calculator functions for most of these prompts.

**Percent correct responses for calculate and decline prompts in English and Chinese**

n.d. &ndash; not determined

| Model | Calculate (en) | Calculate (zh) | Decline (en) | Decline (zh) |
|-|-|-|-|-|
| jedick/functiongemma-langcalc-en.zh | 81 | 87 | 99 | 99 |
| jedick/functiongemma-langcalc-en | 85 | n.d. | 94 | nd. |
| jedick/functiongemma-langcalc-zh | n.d. | 87 | n.d. | 98 |
| google/gemma-4-E2B-it | 94 | 91 | 7 | 18 |

The strong performance of the `en.zh` model fine-tuned on two languages extends to both English and Chinese test data.
Fine-tuning a small model (FunctionGemma 270M) get most of the way to what a much larger base model (Gemma 4 E2B-it) can do on calculator prompts
and surpasses the larger model on decline decisions for non-calculator prompts.
These tests suggest that a single fine-tuned model can be deployed on edge devices to power a voice calculator with different spoken languages.

## Roadmap

✅ Fine-tuned FunctionGemma model\
&emsp;&emsp;🚧 Multilingual support (currently English and Chinese; more languages planned)\
✅ Working demo via Google AI Edge Gallery\
✅ Standalone Android app \
&emsp;&emsp;🚧 Testing and public release

## Repository layout

```
LangCalc/
├── prototype/            # Python prototype
├── gallery-task/         # plugin for Google AI Edge Gallery
├── android-app/          # standalone Android app
└── model/
    ├── data/             # training and test data
    ├── notebooks/        # fine-tuning, inference, and evaluation notebooks
    └── scripts/          # model format conversion scripts
```

## License

See [LICENSE](LICENSE).
