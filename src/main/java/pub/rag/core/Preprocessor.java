/*
 * MIT License
 *
 * Copyright (c) ${YEAR} Qiang Li (李强)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package pub.rag.core;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.rag.core.preprocessor.NamedEntityRecognizer;
import pub.rag.core.preprocessor.QueryEnhancer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class Preprocessor {

	private static final Logger logger = LoggerFactory.getLogger(Preprocessor.class);

	private final NamedEntityRecognizer ner;

	private final QueryEnhancer enhancer;

	/**
	 * Preprocess raw query with NER and query enhancement.
	 *
	 * @param rawQuery original user query
	 * @return enhanced query list
	 * @throws NullPointerException if rawQuery is null
	 * @throws IllegalStateException if NER returns empty result
	 */
	public List<String> process(final String rawQuery, final Map<String, String> outNERs) {
		// Validate input
		Objects.requireNonNull(rawQuery, "rawQuery cannot be null");
		if (rawQuery.isBlank()) {
			throw new IllegalArgumentException("rawQuery cannot be blank");
		}

		// Execute NER
		Map<String, String> entites = ner.recognize(rawQuery);
		if(outNERs != null) {
			outNERs.putAll(entites);
		}
		logger.debug("NER recognition completed, result size: {}", entites == null ? 0 : entites.size());

		// Validate NER result
		if (entites == null || entites.isEmpty()) {
			logger.error("NER returned empty entites for query: {}", rawQuery);
			throw new IllegalStateException("NER entites cannot be empty");
		}

		// Execute enhancement
		List<String> rewritedQueries = enhancer.enhance(entites);
		logger.debug("Query enhancement completed, rewrited query count: {}", rewritedQueries == null ? 0 : rewritedQueries.size());
		if(rewritedQueries == null || rewritedQueries.isEmpty()) {
			logger.error("enhancer returned empty rewritedQueries for query: {}", rawQuery);
		}
		return rewritedQueries;
	}
}