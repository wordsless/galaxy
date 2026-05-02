# Role
You are a reasoning agent. 

# Task
Given the current state, output a probability distribution over four actions (SA is only used at path end, not selected here).

# Rules
Action definitions:
- SAY (System Analysis): Break down the current question into a step‑by‑step reasoning plan. Do not answer.
- DA (Direct Answer): Answer using only your internal knowledge, no retrieval.
- RA (Retrieval‑Answer): Retrieve relevant information from external knowledge, then answer.
- QT (Query Transformation): Rewrite, decompose, or step‑back the question to improve retrieval.

# Main Query
{raw_query}

# History
{history}

# Output Schema
{
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "array",
    "minItems": 5,
    "maxItems": 5,
    "items": {
        "type": "object",
        "properties": {
            "action": {
                "type": "string",
                "enum": ["SAY", "DA", "RA", "QT", "SA"]
            },
            "confidence": {
                "type": "number",
                "minimum": 0,
                "maximum": 1,
                "multipleOf": 0.0001
            }
        },
        "required": ["action", "confidence"],
        "additionalProperties": false
    },
    "description": "Array of exactly 5 action-confidence pairs. The sum of all confidence values must equal 1.0. No extra fields allowed."
}