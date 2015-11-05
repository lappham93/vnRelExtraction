package vn.edu.hcmut.emrre.core.feature;

import java.util.List;
import java.util.regex.Pattern;

import vn.edu.hcmut.emrre.core.entity.Concept;
import vn.edu.hcmut.emrre.core.entity.sentence.Sentence;
import vn.edu.hcmut.emrre.core.entity.sentence.SentenceDAO;
import vn.edu.hcmut.emrre.core.entity.sentence.SentenceDAOImpl;
import vn.edu.hcmut.emrre.core.entity.word.Word;
import vn.edu.hcmut.emrre.core.utils.Constant;
import vn.edu.hcmut.emrre.core.utils.Dictionary;
import vn.edu.hcmut.emrre.core.utils.WordHandle;
import vn.edu.hcmut.emrre.main.EMRCore;

public class FeatureVn {
	private int dimension;
	private double[] vector;

	private Concept preConcept;
	private Concept posConcept;
	private Dictionary conceptDic;
	private Dictionary wordBetweenDic;
	private SentenceDAO senDao;
	private List<Word> lstWord;

	public FeatureVn() {
		this.senDao = new SentenceDAOImpl();
	}

	private int postagEncode(String postag) {
		switch (postag) {
		case "Np":
			return 0;
		case "E":
			return 1;
		case "A":
			return 2;
		case "N":
			return 3;
		case "V":
			return 4;
		case "R":
			return 5;
		case "M":
			return 6;
		case "C":
			return 7;
		case "P":
			return 8;
		case "Nc":
			return 9;
		case "Ny":
			return 10;
		case "X":
			return 11;
		case "T":
			return 12;
		case "L":
			return 13;
		default:
			return -1;

		}
	}

	/**
	 * Context feature
	 */
	private int cfStringMatcher(int idx) {
		String sentence = WordHandle.getSentence(this.preConcept.getFileName(), this.preConcept.getLine());
		if (sentence != null) {
			// define string matchers
			Pattern ppPatt1 = Pattern.compile(Constant.PP_MATCHER);
			Pattern ptePatt1 = Pattern.compile(Constant.PTE_MATCHER_1);
			Pattern ptePatt2 = Pattern.compile(Constant.PTE_MATCHER_2);
			Pattern ptrPatt1 = Pattern.compile(Constant.PTR_MATCHER_1);
			Pattern ptrPatt2 = Pattern.compile(Constant.PTR_MATCHER_2);
			if (this.preConcept.getType() == Concept.Type.PROBLEM
					&& this.posConcept.getType() == Concept.Type.PROBLEM) {
				if (ppPatt1.matcher(sentence.toLowerCase()).find()) {
					this.vector[idx + 0] = 1;
				}
			} else
				if (this.preConcept.getType() == Concept.Type.TEST || this.posConcept.getType() == Concept.Type.TEST) {
				String ctBetween = WordHandle.getWords(this.preConcept.getFileName(), this.preConcept.getLine(),
						this.preConcept.getEnd() + 1, this.posConcept.getBegin() - 1);
				if (ctBetween != null && ptePatt1.matcher(ctBetween.toLowerCase()).find()) {
					this.vector[idx + 1] = 1;
				}
				if (ctBetween != null && ptePatt2.matcher(ctBetween.toLowerCase()).find()) {
					this.vector[idx + 2] = 1;
				}
			} else {
				if (ptrPatt1.matcher(sentence.toLowerCase()).find()) {
					this.vector[idx + 3] = 1;
				}
				if (ptrPatt2.matcher(sentence.toLowerCase()).find()) {
					this.vector[idx + 4] = 1;
				}
			}
		}
		return idx + Constant.CF_STRING_MATCHER_SIZE;
	}

	private int cfWordBetween(int idx) {
		String word;
		int key;
		for (int i = this.preConcept.getEnd() + 1; i < this.posConcept.getBegin(); i++) {
			word = WordHandle.getWord(preConcept.getFileName(), preConcept.getLine(), i);
			key = this.wordBetweenDic.getValue(word);
			if (key != -1) {
				this.vector[key + idx] = 1;
			}
		}
		return idx + this.wordBetweenDic.getSize();
	}

	private int cfPostagBetween(int idx) {
		if (this.lstWord != null) {
			for (int i = this.preConcept.getEnd(); i <= this.posConcept.getBegin(); i++) {
				Word word = this.lstWord.get(i - 1);
				int pos;
				if (word != null) {
					if ((pos = postagEncode(word.getPosTag())) != -1) {
						this.vector[idx + pos] = 1;
					}
				}
			}
		}
		return idx + Constant.CF_POSTAG_BETWEEN_SIZE;
	}

	/**
	 * Single concept feature
	 */
	private int scContent(int idx) {
		String cContent;
		int key;
		cContent = WordHandle.getWords(preConcept.getFileName(), preConcept.getLine(), preConcept.getBegin(),
				preConcept.getEnd());
		key = this.conceptDic.getValue(cContent);
		if (key != -1)
			this.vector[key + idx] = 1;
		cContent = WordHandle.getWords(posConcept.getFileName(), posConcept.getLine(), posConcept.getBegin(),
				posConcept.getEnd());
		key = this.conceptDic.getValue(cContent);
		if (key != -1)
			this.vector[key + idx] = 1;
		return idx + this.conceptDic.getSize();

	}

	private int scType(int idx) {
		int pos;
		if (preConcept.getType() == Concept.Type.PROBLEM) {
			pos = 0;
		} else if (preConcept.getType() == Concept.Type.TEST) {
			pos = 1;
		} else
			pos = 2;
		this.vector[pos + idx] = 1;
		if (posConcept.getType() == Concept.Type.PROBLEM) {
			pos = 3;
		} else if (posConcept.getType() == Concept.Type.TEST) {
			pos = 4;
		} else
			pos = 5;
		this.vector[pos + idx] = 1;

		return idx + Constant.SC_TYPE_SIZE;
	}

	private int scPostag(int idx) {
		if (this.lstWord != null) {
			for (int i = this.preConcept.getBegin(); i <= this.preConcept.getEnd(); i++) {
				Word word = this.lstWord.get(i - 1);
				int pos;
				if (word != null) {
					if ((pos = postagEncode(word.getPosTag())) != -1) {
						this.vector[idx + pos] = 1;
					}
				}
			}
			for (int i = this.posConcept.getBegin(); i <= this.posConcept.getEnd(); i++) {
				Word word = this.lstWord.get(i - 1);
				int pos;
				if (word != null) {
					if ((pos = postagEncode(word.getPosTag())) != -1) {
						this.vector[idx + pos] = 1;
					}
				}
			}
		}
		return idx + Constant.SC_CONCEPT_POSTAG_SIZE;
	}

	/**
	 * Concept vicinity feature
	 */
	private int encode(Concept con) {
		if (con.getType() == Concept.Type.PROBLEM) {
			return 1;
		}
		if (con.getType() == Concept.Type.TEST) {
			return 2;
		}
		return 3;
	}

	private int encode(Concept preCon, Concept posCon) {
		return encode(preCon) * encode(posCon) - 1;
	}

	private int getConceptVicinity(Concept con, boolean before) {
		Concept target = null;
		int min = 0;
		boolean isFind = false;
		for (Concept concept : EMRCore.getConcepts()) {
			if (concept.getFileName().equals(con.getFileName()) && concept.getLine() == con.getLine()) {
				if (before && con.getBegin() > concept.getEnd()) {
					if (!isFind || con.getBegin() - concept.getEnd() < min) {
						target = concept;
						min = con.getBegin() - concept.getEnd();
						isFind = true;
					}
				} else if (!before && con.getEnd() < concept.getBegin()) {
					if (!isFind || concept.getBegin() - con.getEnd() < min) {
						target = concept;
						min = concept.getBegin() - concept.getEnd();
						isFind = true;
					}
				}
			}
		}
		if (!isFind) {
			return -1;
		}
		if (before) {
			return encode(target, con);
		}
		return encode(con, target);
	}

	private int cvConceptType(int idx) {
		int pos = getConceptVicinity(this.preConcept, true);
		if (pos != -1) {
			this.vector[idx + pos] = 1;
		}
		pos = getConceptVicinity(this.posConcept, false);
		if (pos != -1) {
			this.vector[idx + pos + 9] = 1;
		}

		return idx + Constant.CV_CONCEPT_TYPE_SIZE;
	}

	public double[] buildFeatures(Concept preConcept, Concept postConcept) {
		this.preConcept = preConcept;
		this.posConcept = postConcept;
		Sentence sentence = this.senDao.findByRecordAndLineIndex(this.preConcept.getFileName(),
				this.preConcept.getLine());
		if (sentence != null) {
			this.lstWord = sentence.getWords();
		}
		if (this.conceptDic == null) {
			this.conceptDic = new Dictionary();
			this.conceptDic.getDictionaryFromFile1(Constant.CONCEPT_DICTIONARY_FILE_PATH);
		}
		if (this.wordBetweenDic == null) {
			this.wordBetweenDic = new Dictionary();
			this.wordBetweenDic.getDictionaryFromFile1(Constant.WORD_DICTIONARY_FILE_PATH);
		}
		this.dimension = this.conceptDic.getSize() + Constant.SC_TYPE_SIZE + Constant.CF_STRING_MATCHER_SIZE
				+ Constant.CV_CONCEPT_TYPE_SIZE + Constant.CF_POSTAG_BETWEEN_SIZE + this.wordBetweenDic.getSize();
		this.vector = new double[this.dimension];

		int nextIdx;
		nextIdx = scContent(0);
		nextIdx = scType(nextIdx);
		// nextIdx = scPostag(nextIdx);
		nextIdx = cfStringMatcher(nextIdx);
		nextIdx = cfPostagBetween(nextIdx);
		nextIdx = cfWordBetween(nextIdx);
		nextIdx = cvConceptType(nextIdx);

		return this.vector;
	}

}
