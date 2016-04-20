import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import position.Position;
import node.BaseNode;
import node.DoNode;
import node.ForNode;
import node.IfNode;
import node.WhileNode;
import statement.BaseStatement;
import statement.IterationStatement;
import statement.SelectionStatement;
import edge.BaseEdge;
import function.FunctionInfor;

public class CfgTree {

	protected String sourceCode;
	protected List<FunctionInfor> functionList;
	protected List<BaseStatement> statementList;
	protected List<BaseNode> nodeList;
	protected List<BaseEdge> edgeList;

	public CfgTree(String sourceCode) {
		this.sourceCode = sourceCode;
		init();
		createNode();
		setFuctionForNode();
		setParentForNode();
		// sortAscNode();
		createEdge();
	}

	private void init() {
		SyntaxManager syntaxManager = new SyntaxManager(sourceCode);
		this.functionList = syntaxManager.getFunctionList();
		this.statementList = syntaxManager.getStatementList();
		nodeList = new ArrayList<>();
		edgeList = new ArrayList<>();
	}

	public void createNode() {
		int index = 0;
		for (FunctionInfor f : functionList) {
			nodeList.add(new BaseNode(index, f.getName(), f.getContent()));
			index++;
		}
		for (BaseStatement s : statementList) {
			if (s instanceof SelectionStatement) {
				Position exp = ((SelectionStatement) s).getExpression();
				if (s.getType() == Constants.STRUCTURE_IF) {
					IfNode node = new IfNode(index, getSourceAt(exp),
							s.getContent());
					List<Position> state = ((SelectionStatement) s)
							.getStatement();
					if (state.size() == 1) {
						node.setTruePosition(state.get(0));
					} else if (state.size() == 2) {
						node.setTruePosition(state.get(0));
						node.setFalsePosition(state.get(1));
					}
					nodeList.add(node);
				}
			} else if (s instanceof IterationStatement) {
				StringBuilder expContent = new StringBuilder();
				List<Position> expList = ((IterationStatement) s)
						.getExpression();
				Position state = ((IterationStatement) s).getStatement();
				for (Position exp : expList) {
					expContent.append(getSourceAt(exp));
					expContent.append(" ");
				}
				if (s.getType() == Constants.STRUCTURE_DO) {
					DoNode node = new DoNode(index, expContent.toString(),
							s.getContent());
					// Do-while have one expression
					node.setExpressionPosition(expList.get(0));
					node.setStatementPosition(state);
					nodeList.add(node);

				} else if (s.getType() == Constants.STRUCTURE_WHILE) {
					WhileNode node = new WhileNode(index,
							expContent.toString(), s.getContent());
					node.setExpressionPosition(expList.get(0));
					node.setStatementPosition(state);
					nodeList.add(node);
				} else if (s.getType() == Constants.STRUCTURE_FOR) {
					ForNode node = new ForNode(index, expContent.toString(),
							s.getContent());
					node.setExpressionPosition(expList);
					node.setStatementPosition(state);
					nodeList.add(node);
				}
			} else {
				nodeList.add(new BaseNode(index, getSourceAt(s.getContent()), s
						.getContent()));
			}
			index++;
		}
	}

	public void createEdge() {
		for (int i = 0; i < nodeList.size(); i++) {

			if (i + 1 == nodeList.size()
					|| nodeList.get(i).getFunctionId() != nodeList.get(i + 1)
							.getFunctionId())
				continue;

			BaseNode node = nodeList.get(i);
			if (node instanceof IfNode) {
				BaseEdge edge;
				// Create edge true
				int trueId = -1;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					if (((IfNode) node).getTruePosition().cover(startPos)) {
						trueId = j;
						break;
					}
				}
				if (trueId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(trueId));
					edge.setLabel(Constants.LABEL_TRUE);
					edgeList.add(edge);
				}

				// Create edge false
				int falseId = -1;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					if (((IfNode) node).getFalsePosition().cover(startPos)) {
						falseId = j;
						break;
					}
				}
				if (falseId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(falseId));
					edge.setLabel(Constants.LABEL_FALSE);
					edgeList.add(edge);
				}

			} else if (node instanceof WhileNode) {
				BaseEdge edge;
				// Create edge true
				int trueId = -1;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					if (((WhileNode) node).getStatementPosition().cover(
							startPos)) {
						trueId = j;
						break;
					}
				}
				if (trueId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(trueId));
					edge.setLabel(Constants.LABEL_TRUE);
					edgeList.add(edge);
				}

				// Create edge false
				int falseId = -1;
				int minPos = 10000;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					Position pos = ((WhileNode) node).getStatementPosition();
					if (pos.end < startPos && startPos < minPos) {
						falseId = j;
						minPos = startPos;
					}
				}
				if (falseId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(falseId));
					edge.setLabel(Constants.LABEL_FALSE);
					edgeList.add(edge);
				}

			} else if (node instanceof ForNode) {
				BaseEdge edge;
				// Create edge true
				int trueId = -1;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					if (((ForNode) node).getStatementPosition().cover(startPos)) {
						trueId = j;
						break;
					}
				}
				if (trueId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(trueId));
					edge.setLabel(Constants.LABEL_TRUE);
					edgeList.add(edge);
				}

				// Create edge false
				int falseId = -1;
				int minPos = 10000;
				for (int j = 0; j < nodeList.size(); j++) {
					int startPos = nodeList.get(j).getPosition().start;
					Position pos = ((ForNode) node).getStatementPosition();
					if (pos.end < startPos && startPos < minPos) {
						falseId = j;
						minPos = startPos;
					}
				}
				if (falseId != -1) {
					edge = new BaseEdge();
					edge.setNode(node, nodeList.get(falseId));
					edge.setLabel(Constants.LABEL_FALSE);
					edgeList.add(edge);
				}
			} else {
				if (node.isEnd()) {
					BaseNode parentNode = nodeList.get(node.getParentId());
					if (parentNode instanceof IfNode) {
						int nodeId = -1;
						int minPos = 10000;
						for (int j = 0; j < nodeList.size(); j++) {
							int startPos = nodeList.get(j).getPosition().start;
							Position pos = parentNode.getPosition();
							if (pos.end < startPos && startPos < minPos) {
								nodeId = j;
								minPos = startPos;
							}
						}
						if (nodeId != -1) {
							BaseEdge edge = new BaseEdge();
							edge.setNode(node, nodeList.get(nodeId));
							edgeList.add(edge);
						}
					} else if (parentNode instanceof WhileNode) {
						BaseEdge edge = new BaseEdge();
						edge.setNode(node, parentNode);
						edgeList.add(edge);
					} else if (parentNode instanceof ForNode) {
						BaseEdge edge = new BaseEdge();
						edge.setNode(node, parentNode);
						edgeList.add(edge);
					}
				} else {
					BaseEdge edge = new BaseEdge();
					edge.setNode(node, nodeList.get(i + 1));
					edgeList.add(edge);
				}
			}
		}
	}

	public void setFuctionForNode() {
		// Function node have id from 0->functionList's size
		for (int i = 0; i < functionList.size(); i++) {
			Position funcNode = nodeList.get(i).getPosition();
			for (BaseNode node : nodeList) {
				int startPos = node.getPosition().start;
				if (funcNode.cover(startPos))
					node.setFunctionId(i);
			}
		}
	}

	public void setParentForNode() {
		for (BaseNode n : nodeList) {
			Position parentPos = n.getPosition();
			for (BaseNode b : nodeList) {
				Position childPos = b.getPosition();
				if (b != n && parentPos.cover(childPos.start)) {
					b.setParentId(n.getIndex());
				}
			}
		}
		for (int i = 0; i < functionList.size(); i++) {
			nodeList.get(i).setParentId(i);
		}
		for (BaseNode n : nodeList) {
			int start = n.getPosition().start;
			BaseNode parentNode = nodeList.get(n.getParentId());
			int end;
			if (parentNode instanceof WhileNode) {
				end = ((WhileNode) parentNode).getStatementPosition().end;
			} else if (parentNode instanceof DoNode) {
				end = ((DoNode) parentNode).getStatementPosition().end;
			} else if (parentNode instanceof ForNode) {
				end = ((ForNode) parentNode).getStatementPosition().end;
			} else if (parentNode instanceof IfNode) {
				end = ((IfNode) parentNode).getTruePosition().end;
				if (start > end)
					end = ((IfNode) parentNode).getFalsePosition().end;
			} else {
				end = parentNode.getPosition().end;
			}
			Position checkPos = new Position(start, end);
			boolean isEnd = true;
			for (BaseNode b : nodeList) {
				if (b != n && b.getParentId() != n.getIndex()
						&& checkPos.cover(b.getPosition().start)) {
					isEnd = false;
					break;
				}
			}
			n.setEnd(isEnd);
		}
	}

	private String getSourceAt(Position position) {
		return this.sourceCode.substring(position.start, position.end + 1);
	}

	private void sortAscNode() {
		Comparator<BaseNode> comparator = new Comparator<BaseNode>() {
			@Override
			public int compare(BaseNode o1, BaseNode o2) {
				Integer left = o1.getPosition().start;
				Integer right = o2.getPosition().start;
				return left.compareTo(right);
			}
		};
		Collections.sort(this.nodeList, comparator);
	}

	public void printNodeList() {
		for (BaseNode n : nodeList) {
			System.out.println(n.getIndex() + " ----- " + n.getParentId()
					+ " ----- " + n.isEnd() + "\n" + n.getContent());
		}
	}

	public void printEdgeList() {
		for (BaseEdge e : edgeList) {
			System.out.println(e.getSource().getIndex() + " ---> "
					+ e.getDestination().getIndex() + " : " + e.getLabel());
		}
	}
}
