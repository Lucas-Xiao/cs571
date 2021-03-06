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
package edu.emory.mathcs.nlp.component.dep;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.List;

import org.junit.Test;

import edu.emory.mathcs.nlp.common.util.Joiner;
import edu.emory.mathcs.nlp.component.util.node.FeatMap;
import edu.emory.mathcs.nlp.component.util.reader.TSVReader;

/**
 * @author Jinho D. Choi ({@code jinho.choi@emory.edu})
 */
public class DEPNodeTest
{
	@Test
	public void test() throws Exception
	{
		TSVReader<DEPNode> reader = new TSVReader<>(new DEPIndex(1, 2, 3, 4, 5, 6));
		reader.open(new FileInputStream("src/main/resources/dat/wsj_0001.dep"));
		DEPNode[] nodes = reader.next();
		
		// TODO:
		System.out.println(Joiner.join(nodes, "\n"));
	}
	@Test
	public void TestDependent(){
		DEPNode root=new DEPNode();
		DEPNode A = new DEPNode(1,"A","a","DT",new FeatMap("current=a|next=record"));
		DEPNode record=new DEPNode(2,"record","record","NN",new FeatMap());
		DEPNode date=new DEPNode(3,"date","date","NN",new FeatMap());
		DEPNode has=new DEPNode(4,"has","have","VBZ",new FeatMap());
		DEPNode not=new DEPNode(5,"n't","not","RB",new FeatMap());
		DEPNode been=new DEPNode(6,"been","be","VBN",new FeatMap());
		DEPNode set=new DEPNode(7,"set","set","VBN",new FeatMap());
		
		date.addDependent(A,"det");
		date.addDependent(record,"nn");
		set.addDependent(date,"nsubjpass");
		set.addDependent(has,"aux");
		set.addDependent(not,"neg");
		set.addDependent(been,"auxpass");
		root.addDependent(set,"root");
		
		assertEquals(1, A.getID());
		assertEquals("n't",not.getWordForm());
		assertEquals(date.getDependent(1).getPOSTag(),"NN");
		assertEquals(set.getRightMostDependent(),null);
		assertEquals("record",A.getFeat("next"));
		A.removeFeat("current");
		assertEquals(null,A.getFeat("current"));
		List<DEPNode> setdepens=set.getDependentList();
		assertEquals(date,setdepens.get(0));
		assertEquals(A.getHead().getDependentSize(),record.getHead().getDependentSize());
		assertEquals(A.getLowestCommonAncestor(not),set);
		assertEquals(A.getGrandHead(),been.getHead());
		assertEquals(not.getLeftNearestSibling().getRightNearestSibling(),not);
		assertEquals(has.getRightNearestSibling(1),been);
		
				
	}
	@Test
	public void testBasicFields()
	{
		DEPNode node = new DEPNode(1, "Jinho");
		
		assertEquals(1      , node.getID());
		assertEquals("Jinho", node.getWordForm());
		
		node = new DEPNode(1, "Jinho", "jinho", "NNP", new FeatMap("fst=jinho|lst=choi"));
		
		assertEquals(1       , node.getID());
		assertEquals("Jinho" , node.getWordForm());
		assertEquals("jinho" , node.getLemma());
		assertEquals("NNP"   , node.getPOSTag());
		
		node.removeFeat("fst");
		assertEquals(null  , node.getFeat("fst"));
		assertEquals("choi", node.getFeat("lst"));
		
		node.putFeat("fst", "Jinho");
		assertEquals("Jinho", node.getFeat("fst"));
	}
	
	@Test
	public void testSetters()
	{
		DEPNode node1 = new DEPNode(1, "He");
		DEPNode node2 = new DEPNode(2, "bought");
		DEPNode node3 = new DEPNode(3, "a");
		DEPNode node4 = new DEPNode(4, "car");
		
		node2.addDependent(node4, "dobj");
		node2.addDependent(node1, "nsubj");
		node4.addDependent(node3, "det");
		
		List<DEPNode> list = node2.getDependentList();
		assertEquals(node1, list.get(0));
		assertEquals(node4, list.get(1));
	}
}
