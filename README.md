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

The fastest way to try LangCalc is through
[Google AI Edge Gallery](https://github.com/google-ai-edge/gallery),
Google's app for running on-device AI models:

1. Install the AI Edge Gallery app.
2. Add the LangCalc custom task (see [`app/gallery-task/`](app/gallery-task/) for setup steps).
3. Open "Voice Calculator" from the home screen, pick a model, and start talking.

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

For the fine-tuning process, see [`model/notebooks/`](model/notebooks/).

The evaluations compare the fine-tuned FunctionGemma (270M) against a larger general-purpose model
(Gemma 4 E2B) on the same set of spoken calculator requests.
Fine-tuning a small model gets most of the way to what a much larger model can do at a fraction of the size.

Full results and methodology: [`model/evals/results.md`](model/evals/results.md).

## Roadmap

- [x] Fine-tuned FunctionGemma model
- [x] Working demo via Google AI Edge Gallery
- [ ] Hosted demo on Hugging Face (Gradio)
- [ ] Standalone Android app
- [ ] Multilingual support (beyond English)

## Repository layout

```
LangCalc/
├── app/
│   ├── gallery-task/     # plugin for Google AI Edge Gallery
│   └── android/          # standalone Android app [planned]
├── model/
│   ├── notebooks/        # fine-tuning and inference notebooks (Colab)
│   ├── data/             # test data
│   ├── evals/            # evaluation results
└── deploy/
    └── huggingface/      # Gradio demo [planned]
```

## License

See [LICENSE](LICENSE).
