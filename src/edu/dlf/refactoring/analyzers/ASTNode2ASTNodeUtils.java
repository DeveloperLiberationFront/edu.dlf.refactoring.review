package edu.dlf.refactoring.analyzers;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import fj.F;
import fj.F2;
import fj.Ord;
import fj.data.List;
import fj.data.List.Buffer;

public class ASTNode2ASTNodeUtils {
	
	private ASTNode2ASTNodeUtils() throws Exception
	{
		throw new Exception();
	}
	
	public static F<ASTNode, List<ASTNode>> getAncestorStatement = 
		ASTAnalyzer.getAncestorsConditionalFunc.f(new F<ASTNode, Boolean>() {
			@Override
			public Boolean f(ASTNode node) {
				return ASTAnalyzer.isStatement(node);
	}});
	
	public static F<ASTNode, List<ASTNode>> getMethodStatements = 
		new F<ASTNode, List<ASTNode>>() {
		@Override
		public List<ASTNode> f(ASTNode method) {
			ASTNode body = (ASTNode) method.getStructuralProperty(MethodDeclaration.BODY_PROPERTY);
			return FunctionalJavaUtil.createListFromCollection((java.util.List)body.
					getStructuralProperty(Block.STATEMENTS_PROPERTY));
	}};
	
	public static F<ASTNode, ASTNode> getEnclosingStatement = new F<ASTNode,ASTNode>(){
		@Override
		public ASTNode f(ASTNode node) {
			return ASTNode2ASTNodeUtils.getAncestorStatement.f(node).sort
					(Ord.intOrd.comap(ASTNode2IntegerUtils.getLength)).head();
	}};
	
	public static F<ASTNode, ASTNode> getRootFunc = new F<ASTNode, ASTNode>() {
		@Override
		public ASTNode f(ASTNode node) {
			return node.getRoot();
	}};
	
	
	public static final F2<ASTNode, StructuralPropertyDescriptor, List<ASTNode>> 
		getStructuralPropertyFunc = new F2<ASTNode, StructuralPropertyDescriptor, 
			List<ASTNode>>() {
			@Override
			public List<ASTNode> f(ASTNode node, StructuralPropertyDescriptor des) {
				Object prop = node.getStructuralProperty(des);
				if(prop instanceof java.util.List) {
					return List.iterableList((java.util.List)prop);
				}
				if(prop != null && prop instanceof ASTNode) {
					return List.single((ASTNode)prop);
				}
				return List.nil();
	}}; 
	
	private static class NodeCollectorVisitor extends ASTVisitor
	{
		private final F<ASTNode, Boolean> condition;
		private final Buffer<ASTNode> collectedNodeBuffer = Buffer.empty(); 
		
		private NodeCollectorVisitor(F<ASTNode, Boolean> condition)
		{
			this.condition = condition;
		}
		
		
		public void preVisit(ASTNode node)
		{
			if(condition.f(node))
				collectedNodeBuffer.snoc(node);
		}
		
		public List<ASTNode> getCollectedASTNode()
		{
			return collectedNodeBuffer.toList();
		}
	}
	
	
	public static F<ASTNode, ASTNode> getASTNodeBefore = new F<ASTNode, ASTNode>() {
		@Override
		public ASTNode f(final ASTNode target) {
			NodeCollectorVisitor visitor = new NodeCollectorVisitor
				(new F<ASTNode, Boolean>() {
				@Override
				public Boolean f(ASTNode node) {
					return ASTNode2IntegerUtils.getEnd.f(node) < ASTNode2IntegerUtils.
						getStart.f(target); 
			}});
			target.getRoot().accept(visitor);
			return visitor.getCollectedASTNode().sort(Ord.intOrd.comap(
				ASTNode2IntegerUtils.getStart)).last();
	}};
	
	
	public static F<ASTNode, ASTNode> getASTNodeAfter = new F<ASTNode, ASTNode>() {
		@Override
		public ASTNode f(final ASTNode target) {
			NodeCollectorVisitor visitor = new NodeCollectorVisitor(new F<ASTNode, Boolean>() {
				@Override
				public Boolean f(ASTNode node) {
					return ASTNode2IntegerUtils.getEnd.f(target) < ASTNode2IntegerUtils.
						getStart.f(node); 
			}});
			target.getRoot().accept(visitor);
			return visitor.getCollectedASTNode().sort(Ord.intOrd.comap(
				ASTNode2IntegerUtils.getStart)).head();
	}};
}
