/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie.generative
import cc.factorie._
import scala.reflect.Manifest
import scala.collection.mutable.{HashSet,HashMap}
import scala.util.Random

class PlatedDiscreteMixtureTemplate extends GenerativeTemplateWithStatistics3[PlatedDiscreteMixtureVar,MixtureComponents[Proportions],PlatedMixtureChoiceVar] with PlatedMixtureGenerativeTemplate {
  type TemplateType <: PlatedDiscreteMixtureTemplate
  type ChildType <: PlatedDiscreteMixtureVar
  def unroll1(d:PlatedDiscreteMixtureVar) = Factor(d, d.components, d.choice)
  def unroll2(m:MixtureComponents[Proportions]) = for (d <- m.childrenOfClass[PlatedDiscreteMixtureVar]) yield Factor(d, m, d.choice)
  def unroll3(g:PlatedMixtureChoiceVar) = {
    //g.outcomes.foreach(o => { println(o.getClass); println(classOf[DiscreteMixtureSeqVar].isAssignableFrom(o.getClass)) })
    val ret =  for (d <- g.outcomesOfClass[PlatedDiscreteMixtureVar]; if (classOf[PlatedDiscreteMixtureVar].isAssignableFrom(d.getClass))) yield 
      Factor(d, d.components, g)
    //println("DiscreteSeqMixtureTemplate "+ret.size)
    ret
  }
  def prChoosing(s:StatisticsType, seqIndex:Int, mixtureIndex:Int) = pr(s._1(seqIndex).intValue, s._2, mixtureIndex)
  def logprChoosing(s:StatisticsType, seqIndex:Int, mixtureIndex:Int) = math.log(prChoosing(s, seqIndex, mixtureIndex))
  def pr(s:StatisticsType): Double = (0 until s._1.length).foldLeft(1.0)((p:Double,i:Int) => p * pr(s._1(i).intValue, s._2, s._3(i).intValue))
  def pr(s:StatisticsType, seqIndex:Int): Double = pr(s._1(seqIndex).intValue, s._2, s._3(seqIndex).intValue)
  def pr(value:Int, mixture:Seq[Proportions], mixtureIndex:Int): Double = {
    //println("DiscreteMixtureTemplate component="+mixture(mixtureIndex).getClass+" "+mixture(mixtureIndex).getClass.getSuperclass)
    mixture(mixtureIndex).apply(value)
  }
  def logpr(s:StatisticsType) = math.log(pr(s))
  def sampledValue(s:StatisticsType): Seq[DiscreteValue] = sampledValue(s._1(0).domain, s._2, s._3.map(_.intValue))
  def sampledValueChoosing(s:StatisticsType, mixtureIndices:Seq[Int]): Seq[DiscreteValue] = sampledValue(s._1(0).domain, s._2, mixtureIndices)
  def sampledValue(domain:DiscreteDomain, proportions:Seq[Proportions], mixtureIndices:Seq[Int]): Seq[DiscreteValue] =
    mixtureIndices.map(i => domain.getValue(proportions(i).sampleInt))
}
object PlatedDiscreteMixtureTemplate extends PlatedDiscreteMixtureTemplate

trait PlatedDiscreteMixtureVar extends DiscreteSeqVariable with PlatedMixtureGeneratedVar {
  val generativeTemplate = PlatedDiscreteMixtureTemplate
  def generativeFactor = new PlatedDiscreteMixtureTemplate.Factor(this, components, choice)
  private var _components: FiniteMixture[Proportions] = null
  def components: FiniteMixture[Proportions] = _components
  def setComponents(c:FiniteMixture[Proportions]): Unit = {
    if (_components ne null) _components.removeChild(this)(null)
    _components = c
    _components.addChild(this)(null)
  }
  private var _choice: PlatedMixtureChoiceVar = null
  def choice: PlatedMixtureChoiceVar = _choice
  def setChoice(c:PlatedMixtureChoiceVar): Unit = {
    if (_choice ne null) _choice.removeOutcome(this)
    _choice = c
    _choice.addOutcome(this)
  }
  //def chosenParents = List(components(choice.intValue))
  //def proportions = components(choice.intValue)
  // override for efficiency
  override def prChoosing(seqIndex:Int, mixtureIndex:Int) = components(choice.intValue(seqIndex)).apply(this.intValue(seqIndex)) 
  //override def sampledValueChoosing(mixtureIndex:Int): Value = domain.getValue(components(choice.intValue).sampleInt)
}

abstract class PlatedDiscreteMixture(components: FiniteMixture[Proportions], choice:PlatedMixtureChoiceVar, initialValue:Seq[Int] = Nil) 
         extends DiscreteSeqVariable(initialValue) with PlatedDiscreteMixtureVar with GeneratedVar 
{
  setComponents(components)
  setChoice(choice)
}

abstract class PlatedCategoricalMixture[A](components: FiniteMixture[Proportions], choice:PlatedMixtureChoiceVar, initialValue:Seq[A] = Nil) 
         extends CategoricalSeqVariable(initialValue) with PlatedDiscreteMixtureVar with GeneratedVar 
{
  setComponents(components)
  setChoice(choice)
}