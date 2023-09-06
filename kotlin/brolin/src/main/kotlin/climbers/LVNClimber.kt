package climbers

import trees.*

sealed interface BrilValue

data class Const(val value: Value) : BrilValue

/** Represents a computed value in a Bril program. */
data class ValueTuple(val op: Operator, val args: List<Int>) : BrilValue

class LVNClimber {

    fun lvnProgram(program: CookedProgram): List<List<BasicBlock>> {
        val freshNames = FreshNameClimber(program)
        val basicClimber = BlockClimber()
        return basicClimber.basicBlocker(program).map {
            it.map { block -> lvn(block, freshNames) }
        }
    }

    private fun lvn(basicBlock: BasicBlock, freshNames: FreshNameClimber): BasicBlock {
        val tupleToNum: MutableMap<BrilValue, Int> = hashMapOf()  // map to value numbers
        val numToValueCanonicalVar: ArrayList<Pair<BrilValue, String>> =
            arrayListOf()  // map to value tuples, where index is value number
        val env: MutableMap<String, Int> = hashMapOf()  // mapping from variable names to current value number

        /** Modifies table and environment using the provided BrilValue tuple, and adds appropriate instructions
         *  to the block. */
        fun processValue(
            inst: WriteInstruction,
            value: BrilValue,
            blockBuilder: MutableList<CookedInstructionOrLabel>
        ) {
            // It is safe use the list size as the candidate fresh value number since we never remove elements
            val num = tupleToNum.getOrPut(value) { numToValueCanonicalVar.size }
            env[inst.dest] = num

            if (num == numToValueCanonicalVar.size) {
                // Newly computed value since we added to the table.
                numToValueCanonicalVar.add(Pair(value, inst.dest))
                // Add the instruction, modified to use canonical variables as arguments
                when (inst) {
                    is ConstantInstruction -> blockBuilder.add(inst)
                    is ValueOperation -> {
                        blockBuilder.add(
                            ValueOperation(
                                op = inst.op,
                                dest = inst.dest,
                                type = inst.type,
                                args = inst.args.map { numToValueCanonicalVar[env[it]!!].second },
                                funcs = inst.funcs,
                                labels = inst.labels
                            )
                        )
                    }
                }
            } else {
                // The value has been computed before; reuse it.
                blockBuilder.add(
                    ValueOperation(
                        op = Operator.ID,
                        dest = inst.dest,
                        type = inst.type,
                        args = listOf(numToValueCanonicalVar[num].second)
                    )
                )
            }
        }

        fun processCopy(
            inst: ValueOperation,
            blockBuilder: MutableList<CookedInstructionOrLabel>
        ) {
            assert(inst.op == Operator.ID)
            val copy = env[inst.args[0]]
            assert(copy != null)  // We should reference an existing variable
            env[inst.dest] = copy!!  // Amend the environment; no need to add to the table
            blockBuilder.add(inst)
        }

        val result: ArrayList<CookedInstructionOrLabel> =
            basicBlock.instructions.fold(initial = arrayListOf()) { acc, inst ->
                if (inst is WriteInstruction) {
                    val numOfOverwrittenValue = env[inst.dest]
                    if (numOfOverwrittenValue != null) {  // we are indeed overwriting something
                        val (value, oldCanonicalVar) = numToValueCanonicalVar[numOfOverwrittenValue]
                        if (oldCanonicalVar == inst.dest) {
                            // Update table to use new canonical variable
                            val newCanonicalVar = freshNames.get(base = oldCanonicalVar)
                            numToValueCanonicalVar[numOfOverwrittenValue] = Pair(value, newCanonicalVar)
                            // Declare new canonical variable to be copy of old value
                            acc.add(
                                ValueOperation(
                                    op = Operator.ID,
                                    dest = newCanonicalVar,
                                    type = inst.type,
                                    args = listOf(oldCanonicalVar)
                                )
                            )
                        }
                    }
                }

                when (inst) {
                    is ConstantInstruction -> processValue(inst, Const(inst.value), acc)
                    is ValueOperation -> {
                        when (inst.op) {
                            // Values with side effects
                            Operator.CALL, Operator.PHI, Operator.ALLOC, Operator.LOAD, Operator.PTRADD -> acc.add(inst)
                            // Values that are always equivalent to an existing value
                            Operator.ID -> processCopy(inst, acc)
                            // Possibly new values
                            else -> {
                                processValue(
                                    inst,
                                    ValueTuple(
                                        op = inst.op,
                                        args = (if (inst.op.commutative) inst.args.sorted() else inst.args).map {
                                            assert(env[it] != null)
                                            env[it]!!
                                        }),
                                    acc
                                )
                            }
                        }
                    }
                    is CookedLabel -> acc.add(inst)
                    is EffectOperation -> acc.add(
                        EffectOperation(
                            op = inst.op,
                            args = inst.args.map { numToValueCanonicalVar[env[it]!!].second },
                            funcs = inst.funcs,
                            labels = inst.labels
                        )
                    )
                }
                acc
            }
        return BasicBlock(result)
    }
}