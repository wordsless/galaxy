# Sampling Parameters:
{
    "do_sample": true,
    "temperature": 0.6,
    "top_p": 0.9,
    "num_outputs": 3,
    "max_tokens": 128,
    "frequency_penalty": 0.5,
    "presence_penalty": 0.5,
    "best_of": 3
}

# Role:
You are a retrieval assistant.

# Task:
Generate a list of specific, well‑formed search queries to retrieve relevant information from an external knowledge base. The queries should help answer the current question.

# Rules:
- 1. Each query must be a standalone, clear search phrase.
- 2. Avoid re‑retrieving information already present in history.
- 3. If no well‑formed search phrase is needed, output "None" (a JSON string).
- 4. Otherwise, output a JSON array of query strings, e.g., ["query1", "query2"].
- 5. Output only the JSON value (string or array) following the Output Schema. No extra text, comments, or formatting.

# Main Query:
{main_query}

# Current Question:
{current_question}

# History:
{history}

# Output Schema:
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "oneOf": [
        {
            "type": "string",
            "const": "None",
            "description": "No well‑formed search phrase."
        },
        {
            "type": "array",
            "items": { "type": "string" },
            "minItems": 1,
            "uniqueItems": true,
            "description": "Array of well‑formed search phrases."
        }
    ]
}