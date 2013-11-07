package edu.dlf.refactoring.analyzers;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import fj.Equal;
import fj.F;

public class ASTNodeEq {
	private ASTNodeEq() throws Exception
	{
		throw new Exception();
	}
	
	public static Equal<ASTNode> ReferenceEq = Equal.equal(
			new F<ASTNode, F<ASTNode, Boolean>>() {
				@Override
				public F<ASTNode, Boolean> f(final ASTNode node1) {
					return new F<ASTNode, Boolean>() {
						@Override
						public Boolean f(final ASTNode node2) {
							return node1 == node2;
	}};}});
		
	
	public static Equal<ASTNode> TypeDeclarationNameEq = Equal.stringEqual.comap(
		new F<ASTNode, String>() {
			@Override
			public String f(ASTNode type) {
				return type.getStructuralProperty(TypeDeclaration.NAME_PROPERTY).
					toString();
	}});
	
	
	public static Equal<ASTNode> MethodDeclarationNameEq = Equal.equal(
		new F<ASTNode, F<ASTNode, Boolean>>() {
			@Override
			public F<ASTNode, Boolean> f(final ASTNode node1) {
				return new F<ASTNode, Boolean>() {
					@Override
					public Boolean f(final ASTNode node2) {
						return ASTAnalyzer.getMethodDeclarationNamesEqualFunc().
							f(node1, node2);
	}};}});
}