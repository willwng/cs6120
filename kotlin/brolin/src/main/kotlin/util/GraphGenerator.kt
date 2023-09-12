package util

import dataflow.DataflowAnalysis.DataflowResult

object GraphGenerator {
    private fun String.fixStringQuotes(): String {
        return replace(Regex("\""), Regex.escapeReplacement("\\\""))
    }

    private fun BasicBlock.getContent(): String {
        val sb = StringBuilder()
        instructions.forEach { sb.append("$it \\l") }
        return sb.toString().fixStringQuotes()
    }

    private fun getNodeName(node: CFGNode, nodeMap: Map<CFGNode, Int>): String {
        return "node_${nodeMap[node]!!}"
    }

    private fun addNode(
        node: CFGNode,
        nodeMap: Map<CFGNode, Int>,
        dotWriter: DotWriter,
        prologue: String = "",
        epilogue: String = ""
    ) {
        val content =
            prologue.fixStringQuotes() + "\\l\\l" + node.block.getContent() + "\\l" + epilogue.fixStringQuotes()
        dotWriter.writeNode(nodeName = getNodeName(node, nodeMap), shape = "box", label = content)
    }

    private fun addEdge(
        source: CFGNode,
        sink: CFGNode,
        nodeMap: Map<CFGNode, Int>,
        dotWriter: DotWriter,
    ) {
        dotWriter.writeEdge(
            sourceName = getNodeName(source, nodeMap),
            sinkName = getNodeName(sink, nodeMap),
        )
    }

    fun <T> createGraphOutput(cfg: CFG, dataflowResult: DataflowResult<T>? = null): DotWriter {
        val dotWriter = DotWriter()
        dotWriter.startGraph(name = cfg.function.name)

        // Keep track of the numbering of each CFG node
        val nodeMap = cfg.nodes.mapIndexed { i, node -> node to i }.toMap()
        // Add the nodes, then the edges
        cfg.nodes.forEach { node ->
            addNode(
                node = node,
                nodeMap = nodeMap,
                dotWriter = dotWriter,
                prologue = if (dataflowResult != null) "in: ${dataflowResult.result[node]?.first}" else "",
                epilogue = if (dataflowResult != null) "out: ${dataflowResult.result[node]?.second}" else ""
            )
        }
        cfg.nodes.forEach { node ->
            node.successors.forEach { sink ->
                addEdge(
                    source = node,
                    sink = sink,
                    nodeMap = nodeMap,
                    dotWriter = dotWriter
                )
            }
        }
        dotWriter.writeEntryEdge(getNodeName(node = cfg.entry, nodeMap = nodeMap))
        dotWriter.finishGraph()

        return dotWriter
    }
}