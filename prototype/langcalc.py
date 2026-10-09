# LangCalc: voice-driven calculator with FunctionGemma
# 20260812 jmd first version
# 20261009 add decline() to tools; add model_family_adapters

from transformers import AutoProcessor, AutoModelForCausalLM
import torch
from tools import *
from model_family_adapters import detect_model_family, FAMILY_CONFIGS

## Login to Hugging Face Hub
#from huggingface_hub import login
#login()

# Uncomment one line to choose the model:
# Gemma 4 or base or fine-tuned FunctionGemma
GEMMA_MODEL_ID = "google/gemma-4-E2B-it"
#GEMMA_MODEL_ID = "google/functiongemma-270m-it"
#GEMMA_MODEL_ID = "jedick/functiongemma-langcalc-en.zh"

processor = AutoProcessor.from_pretrained(GEMMA_MODEL_ID, device_map="auto")
model = AutoModelForCausalLM.from_pretrained(GEMMA_MODEL_ID, dtype="auto", device_map="auto")

def run_inference(model, processor, family, tools, prompt, max_new_tokens=128):
    """
    Run inference to generate function calls

    Returns:
       Function call or None for no function call generated
    """
    cfg = FAMILY_CONFIGS[family]
    messages = [
        {"role": cfg["system_role"], "content": cfg["system_message"]},
        {"role": "user", "content": prompt},
    ]
    inputs = cfg["prepare_inputs"](processor, messages, tools, model)

    with torch.no_grad():
        out = model.generate(
            **inputs, max_new_tokens=max_new_tokens, **cfg["generate_kwargs"](processor)
        )

    generated_tokens = out[0][len(inputs["input_ids"][0]):]
    text = processor.decode(generated_tokens, **cfg["decode_kwargs"])
    calls = cfg["extract_tool_calls"](text)
    return calls if calls else None

# The main interface

def ask(prompt):
    """
    Generates tool calls and runs calculation functions based on the prompt

    Args:
        prompt: user's prompt

    Returns:
        None (prints tool call and calculation result)
    """

    family = detect_model_family(GEMMA_MODEL_ID)
    
    # Generate tool calls
    calls = run_inference(model, processor, family, tools, prompt)

    if calls:
        # Print the function call and the result
        for c in calls:
            print(c)
            result = globals()[c['name']](**c['arguments'])
            print(result)

    else:
        print("--- no tool call generated ---")

