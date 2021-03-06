package edu.dlf.refactoring.hiding;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.dom.ASTNode;

import com.google.inject.Inject;

import edu.dlf.refactoring.design.IDetectedRefactoring.NodeListDescriptor;
import edu.dlf.refactoring.refactorings.DetectedRenameTypeRefactoring;

public class RenameTypeHider extends AbstractRenameHider{

	@Inject
	public RenameTypeHider(Logger logger) {
		super(logger);
	}

	@Override
	protected NodeListDescriptor getBeforeNamesDescriptor() {
		return DetectedRenameTypeRefactoring.SimpleNamesBefore;
	}

	@Override
	protected NodeListDescriptor getAfterNamesDescriptor() {
		return DetectedRenameTypeRefactoring.SimpleNamesAfter;
	}

	@Override
	protected boolean isSimpleNameToUpdate(ASTNode name) {
		return true;
	}

}
