# Task
Accurately extract all named entities from user query, mark corresponding semantic entity types.

# Requirements
1. Do not restrict fixed entity categories, extract all valid proper nouns.
2. Never split long professional words, brand names and organization names.
3. Entity types include brand, organization, person, location, time, product, professional_term.
4. Keep semantic accuracy, no missing entities, no redundant invalid entities.

# Output Format
Only output pure JSON array: [{"word":"entity_type"}]
No explanation, no comments, no extra content.

User Query:
{user_query}