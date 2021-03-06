/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2016 Niels Basjes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package nl.basjes.parse.useragent.analyze.treewalker.steps;

import nl.basjes.parse.useragent.UserAgentParser;
import nl.basjes.parse.useragent.UserAgentParser.CommentSeparatorContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.basjes.parse.useragent.utils.AntlrUtils.getSourceText;

public abstract class Step {
    protected static final Logger LOG = LoggerFactory.getLogger(Step.class);
    private int stepNr;
    protected String logprefix = "";
    private Step nextStep;

    protected boolean verbose = false;

    public void setVerbose(boolean newVerbose) {
        this.verbose = newVerbose;
    }

    public final void setNextStep(int newStepNr, Step newNextStep) {
        this.stepNr = newStepNr;
        this.nextStep = newNextStep;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < newStepNr + 1; i++) {
            sb.append("-->");
        }
        logprefix = sb.toString();
    }

    protected final String walkNextStep(ParseTree tree, String value) {
        if (nextStep == null) {
            String result = value;
            if (value == null) {
                result = GetResultValueVisitor.getResultValue(tree);
            }
            if (verbose) {
                LOG.info("{} Final (implicit) step: {}", logprefix, result);
            }
            return result;
        }

        if (verbose) {
            LOG.info("{} Tree: >>>{}<<<", logprefix, getSourceText(tree));
            LOG.info("{} Enter step({}): {}", logprefix, stepNr, nextStep);
        }
        String result = nextStep.walk(tree, value);
        if (verbose) {
            LOG.info("{} Leave step ({}): {}", logprefix, result == null ? "-" : "+", nextStep);
        }
        return result;
    }

    protected final ParseTree up(ParseTree tree) {
        if (tree == null) {
            return null;
        }

        // Needed because of the way the ANTLR rules have been defined.
        if (tree instanceof UserAgentParser.ProductNameWordsContext     ||
            tree instanceof UserAgentParser.ProductNameEmailContext     ||
            tree instanceof UserAgentParser.ProductNameUuidContext      ||
            tree instanceof UserAgentParser.ProductNameKeyValueContext  ||
            tree instanceof UserAgentParser.ProductNameVersionContext
            ) {
            return up(tree.getParent());
        }
        return tree.getParent();
    }

    protected final boolean treeIsSeparator(ParseTree tree) {
        return tree instanceof CommentSeparatorContext
//            || tree instanceof NestedCommentSeparatorContext
            || tree instanceof TerminalNode;
    }

    protected String getActualValue(ParseTree tree, String value) {
        if (value == null) {
            return getSourceText(tree);
        }
        return value;
    }

    protected Integer tokenToInteger(Token token) {
        if (token == null) {
            return null;
        }

        try {
            return Integer.parseInt(token.getText());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * This will walk into the tree and recurse through all the remaining steps.
     * This must iterate of all possibilities and return the first matching result.
     *
     * @param tree  The tree to walk into.
     * @param value The string representation of the previous step (needed for compare and lookup operations).
     *              The null value means to use the implicit 'full' value (i.e. getSourceText(tree) )
     * @return Either null or the actual value that was found.
     */
    public abstract String walk(ParseTree tree, String value);

    public Step getNextStep() {
        return nextStep;
    }
}


