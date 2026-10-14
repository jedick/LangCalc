# LangCalc: voice-driven calculator with FunctionGemma
# 20260812 jmd first version
# 20261009 split out tools.py and add model_family_adapters.py
# 20261013 add print arguments to view messages and text

from transformers import AutoProcessor, AutoModelForCausalLM
import torch
from tools import *
from model_family_adapters import detect_model_family, FAMILY_CONFIGS

## Login to Hugging Face Hub
#from huggingface_hub import login
#login()

# Uncomment one line to choose the model:
# Gemma 4 or base or fine-tuned FunctionGemma
#GEMMA_MODEL_ID = "google/gemma-4-E2B-it"
GEMMA_MODEL_ID = "google/functiongemma-270m-it"
#GEMMA_MODEL_ID = "jedick/functiongemma-langcalc-en.zh"

# Get the model family and config
family = detect_model_family(GEMMA_MODEL_ID)
family_config = FAMILY_CONFIGS[family]

# Load the processor (falls back to the tokenizer for text-only models) and model
processor = AutoProcessor.from_pretrained(GEMMA_MODEL_ID)
model = AutoModelForCausalLM.from_pretrained(
    GEMMA_MODEL_ID, dtype="auto", device_map="auto", **family_config["model_kwargs"]
)

def run_inference(user_text, *, print_messages=False, print_prompt_text=False,
                  print_generated_text=False, max_new_tokens=128):
    """
    Run inference to generate function calls

    Args:
        print_messages: print the system message and user message
        print_text: print the full prompt as the model sees it
        print_output_text: print the generated text

    Returns:
        Function call or None for no function call generated
    """

    # The "prompt" half of a prompt-completion pair; the model generates the completion
    messages = [
        {"role": family_config["system_role"], "content": family_config["system_message"]},
        {"role": "user", "content": user_text},
    ]
    if print_messages:
        print(messages)

    # Render the chat template to text (no tokenization) so we can see what the model sees
    prompt_text = processor.apply_chat_template(
        messages, tools=tools, add_generation_prompt=True, tokenize=False
    )
    if print_prompt_text:
        print(prompt_text)

    # Tokenize and move the tensors to the device
    # The chat template for both model families starts the text with <bos>;
    # add_special_tokens=False prevents FunctionGemma's tokenizer from adding a second <bos>
    inputs = processor(text=prompt_text, add_special_tokens=False, return_tensors="pt").to(model.device)

    with torch.no_grad():
        output_ids = model.generate(
            **inputs, max_new_tokens=max_new_tokens, **family_config["generate_kwargs"](processor)
        )

    # generate() returns the prompt followed by the new tokens; keep the new tokens
    generated_ids = output_ids[0][len(inputs["input_ids"][0]):]
    # Keep special tokens so the tool-call tags are never stripped and the stop token is visible
    generated_text = processor.decode(generated_ids, skip_special_tokens=False)
    if print_generated_text:
        print(generated_text)

    tool_calls = family_config["extract_tool_calls"](generated_text)
    return tool_calls if tool_calls else None

# The main interface

def ask(user_text, **print_options):
    """
    Generates tool calls and runs calculation functions based on the prompt

    Args:
        user_text: user's prompt

    Returns:
        None (prints tool call and calculation result)
    """

    # Generate tool calls
    tool_calls = run_inference(user_text, **print_options)
    if tool_calls:
        # Print the function call and the result
        tool_functions = {f.__name__: f for f in tools}
        for call in tool_calls:
            print(call)
            print(tool_functions[call["name"]](**call["arguments"]))
    else:
        print("--- no tool call generated ---")


# Example usage
# Examples are not commented but placed in 'if' statements for easy copying

# Prints function call and result
if False:
    ask("what's 2+2?")

# Prints system and user messages and prompt and generated text for debugging
if False:
    VERBOSE = dict(print_messages=True, print_prompt_text=True, print_generated_text=True)
    ask("what's 2+2?", **VERBOSE)
