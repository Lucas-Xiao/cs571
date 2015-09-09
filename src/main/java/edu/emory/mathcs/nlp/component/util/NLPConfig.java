/**
 * Copyright 2015, Emory University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.mathcs.nlp.component.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.InputStream;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.emory.mathcs.nlp.common.util.Language;
import edu.emory.mathcs.nlp.common.util.XMLUtils;
import edu.emory.mathcs.nlp.component.util.reader.TSVIndex;
import edu.emory.mathcs.nlp.component.util.reader.TSVReader;
import edu.emory.mathcs.nlp.learn.model.StringModel;
import edu.emory.mathcs.nlp.learn.sgd.StochasticGradientDescent;
import edu.emory.mathcs.nlp.learn.sgd.adagrad.AdaGradHinge;
import edu.emory.mathcs.nlp.learn.sgd.adagrad.BinomialAdaGradHinge;
import edu.emory.mathcs.nlp.learn.sgd.adagrad.MultinomialAdaGradHinge;
import edu.emory.mathcs.nlp.learn.sgd.perceptron.BinomialPerceptron;
import edu.emory.mathcs.nlp.learn.sgd.perceptron.MultinomialPerceptron;
import edu.emory.mathcs.nlp.learn.sgd.perceptron.Perceptron;

/**
 * @author Jinho D. Choi ({@code jinho.choi@emory.edu})
 */
public abstract class NLPConfig<N> implements ConfigXML
{
	private NLPMode mode;
	private Element xml;
	
//	=================================== CONSTRUCTORS ===================================
	
	public NLPConfig(NLPMode mode)
	{
		setMode(mode);
	}
	
	public NLPConfig(NLPMode mode, InputStream in)
	{
		setMode(mode);
		xml = XMLUtils.getDocumentElement(in);
	}
	
//	=================================== GETTERS & SETTERS ===================================  

	public NLPMode getMode()
	{
		return mode;
	}
	
	public void setMode(NLPMode mode)
	{
		this.mode = mode;
	}
	
	public Language getLanguage()
	{
		String language = XMLUtils.getTextContentFromFirstElementByTagName(xml, LANGUAGE);
		return Language.getType(language);
	}
	
	public boolean isBootstrap()
	{
		Element eMode = getModeElement();
		return XMLUtils.getBooleanTextContentFromFirstElementByTagName(eMode, BOOTSTRAP);
	}

	public TSVReader<N> getTSVReader()
	{
		return new TSVReader<>(getTSVIndex());
	}
	
	/** Called by {@link #getTSVReader()}. */
	protected Object2IntMap<String> getFieldMap(Element eTSV)
	{
		NodeList list = eTSV.getElementsByTagName(COLUMN);
		int i, index, size = list.getLength();
		Element element;
		String field;
		
		Object2IntMap<String> map = new Object2IntOpenHashMap<>();
		
		for (i=0; i<size; i++)
		{
			element = (Element)list.item(i);
			field   = XMLUtils.getTrimmedAttribute(element, FIELD);
			index   = XMLUtils.getIntegerAttribute(element, INDEX);
			
			map.put(field, index);
		}
		
		return map;
	}
	
	protected abstract TSVIndex<N> getTSVIndex();
	
//	=================================== TRAINER ===================================
	
	public StochasticGradientDescent[] getLearners(StringModel[] models)
	{
		StochasticGradientDescent[] trainers = new StochasticGradientDescent[models.length];
		Element eMode = getModeElement();
		
		for (int i=0; i<models.length; i++)
			trainers[i] = getLearner(eMode, models[i], i);
		
		return trainers;
	}
	
	private StochasticGradientDescent getLearner(Element eMode, StringModel model, int index)
	{
		Element  eTrainer = XMLUtils.getElementByTagName(eMode, TRAINER, index);
		String  algorithm = XMLUtils.getTrimmedAttribute(eTrainer, ALGORITHM);
		initTrainer(eTrainer, model);
		
		switch (algorithm)
		{
		case PERCEPTRON   : return getPerceptron  (eTrainer, model);
		case ADAGRAD_HINGE: return getAdaGradHinge(eTrainer, model);
		}
		
		throw new IllegalArgumentException(algorithm+" is not a valid algorithm name.");
	}
	
	private void initTrainer(Element eTrainer, StringModel model)
	{
		int labelCutoff   = XMLUtils.getIntegerAttribute(eTrainer, LABEL_CUTOFF);
		int featureCutoff = XMLUtils.getIntegerAttribute(eTrainer, FEATURE_CUTOFF);
		float bias        = XMLUtils.getFloatAttribute  (eTrainer, BIAS);
		boolean reset     = XMLUtils.getBooleanAttribute(eTrainer, RESET);
		
		model.setBias(bias);
		model.vectorize(labelCutoff, featureCutoff, reset);
	}
	
	private Perceptron getPerceptron(Element eTrainer, StringModel model)
	{
		boolean binomial     = XMLUtils.getBooleanAttribute(eTrainer, BINOMIAL);
		boolean average      = XMLUtils.getBooleanAttribute(eTrainer, AVERAGE);
		double  learningRate = XMLUtils.getDoubleAttribute (eTrainer, LEARNING_RATE);
		
		return binomial ? new BinomialPerceptron(model.getWeightVector(), average, learningRate) : new MultinomialPerceptron(model.getWeightVector(), average, learningRate);
	}
	
	private AdaGradHinge getAdaGradHinge(Element eTrainer, StringModel model)
	{
		boolean binomial     = XMLUtils.getBooleanAttribute(eTrainer, BINOMIAL);
		boolean average      = XMLUtils.getBooleanAttribute(eTrainer, AVERAGE);
		double  learningRate = XMLUtils.getDoubleAttribute (eTrainer, LEARNING_RATE);
		double  ridge        = XMLUtils.getDoubleAttribute (eTrainer, RIDGE);
		
		return binomial ? new BinomialAdaGradHinge(model.getWeightVector(), average, learningRate, ridge) : new MultinomialAdaGradHinge(model.getWeightVector(), average, learningRate, ridge);
	}
	
//	=================================== XML ===================================

	protected Element getModeElement(NLPMode mode)
	{
		NodeList list = xml.getChildNodes();
		int i, len = list.getLength();
		Node node;
		
		for (i=0; i<len; i++)
		{
			node = list.item(i);
			if (node.getNodeName().equals(mode.toString()))
				return (Element)node;
		}
		
		return null;
	}
	
	protected Element getModeElement()
	{
		return getModeElement(mode);
	}
}
