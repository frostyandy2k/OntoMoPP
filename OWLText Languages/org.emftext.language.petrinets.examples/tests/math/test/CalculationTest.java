package math.test;

import static org.junit.Assert.*;
import math.Element;
import math.Expression;
import math.MathFactory;
import math.impl.ExpressionImpl;
import math.semantics.MathSemanticsEvaluation;

import org.junit.Test;

public class CalculationTest {

	private MathFactory factory = MathFactory.eINSTANCE;

	@Test
	public void testSimpleExpression() {
		Element simpleElement = factory.createElement();
		simpleElement.setValue(2);
		MathSemanticsEvaluation mathSemanticsEvaluation = new MathSemanticsEvaluation();
		mathSemanticsEvaluation.add_to_place_InitialPlace(simpleElement);
		mathSemanticsEvaluation.evaluateSemantics();
		assertEquals(simpleElement.getValue(), 2);
	}

	@Test
	public void testAddExpression() {
		Element three = factory.createElement();
		three.setValue(3);
	
		Element two = factory.createElement();
		two.setValue(2);
		
		Expression add = factory.createExpression();
		add.setOperator("+");
		add.getElements().add(three);
		add.getElements().add(two);
		
		MathSemanticsEvaluation mathSemanticsEvaluation = new MathSemanticsEvaluation();
		mathSemanticsEvaluation.add_to_place_InitialPlace(add);
		mathSemanticsEvaluation.add_to_place_InitialPlace(two);
		mathSemanticsEvaluation.add_to_place_InitialPlace(three);
		mathSemanticsEvaluation.evaluateSemantics();
		
		assertEquals(5, add.getValue());
	}
	
	@Test
	public void testNestedExpression() {
		Element three = factory.createElement();
		three.setValue(3);
	
		Element two = factory.createElement();
		two.setValue(2);
		
		Expression add = factory.createExpression();
		add.setOperator("+");
		add.getElements().add(three);
		add.getElements().add(two);
		
		Expression mult = factory.createExpression();
		mult.setOperator("*");
		mult.getElements().add(add);
		
		Element six = factory.createElement();
		six.setValue(6);
		mult.getElements().add(six);
		
		MathSemanticsEvaluation mathSemanticsEvaluation = new MathSemanticsEvaluation();
		mathSemanticsEvaluation.add_to_place_InitialPlace(add);
		mathSemanticsEvaluation.add_to_place_InitialPlace(mult);
		mathSemanticsEvaluation.add_to_place_InitialPlace(two);
		mathSemanticsEvaluation.add_to_place_InitialPlace(three);
		mathSemanticsEvaluation.add_to_place_InitialPlace(six);
		mathSemanticsEvaluation.evaluateSemantics();
		
		assertEquals(5, add.getValue());
		assertEquals(30, mult.getValue());
	}
	
	@Test
	public void testAutonInit() {
		Element three = factory.createElement();
		three.setValue(3);
	
		Element two = factory.createElement();
		two.setValue(2);
		
		Expression add = factory.createExpression();
		add.setOperator("+");
		add.getElements().add(three);
		add.getElements().add(two);
		
		Expression mult = factory.createExpression();
		mult.setOperator("*");
		mult.getElements().add(add);
		
		Element six = factory.createElement();
		six.setValue(6);
		mult.getElements().add(six);
		
		MathSemanticsEvaluation mathSemanticsEvaluation = new MathSemanticsEvaluation();
		mathSemanticsEvaluation.intialisePlaces(mult);
		mathSemanticsEvaluation.evaluateSemantics();
		
		assertEquals(5, add.getValue());
		assertEquals(30, mult.getValue());
	}

	
}
