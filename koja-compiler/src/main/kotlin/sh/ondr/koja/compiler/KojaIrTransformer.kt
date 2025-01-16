package sh.ondr.koja.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

@OptIn(UnsafeDuringIrConstructionAPI::class)
class KojaIrTransformer(
	isTest: Boolean,
	private val pluginContext: IrPluginContext,
	private val logger: MessageCollector,
) : IrElementTransformerVoid() {
	val pkg = "sh.ondr.koja"
	val initializerFq = if (isTest) "$pkg.generated.initializer.KojaTestInitializer" else "$pkg.generated.initializer.KojaInitializer"
	private val initializerClassId = ClassId.topLevel(FqName(initializerFq))
	private val functionsToInject = setOf(
		FqName("sh.ondr.koja.jsonSchema"),
		FqName("sh.ondr.koja.toSchema"),
	)

	override fun visitCall(expression: IrCall): IrExpression {
		expression.transformChildrenVoid()

		val callee = expression.symbol.owner
		val functionFqName = callee.fqNameWhenAvailable

		if (functionFqName in functionsToInject) {
			val initializerSymbol = pluginContext.referenceClass(initializerClassId) ?: error("Could not find KojaInitializer")

			// Insert a reference to KojaInitializer before the call
			val builder = DeclarationIrBuilder(
				pluginContext,
				symbol = callee.symbol,
				startOffset = expression.startOffset,
				endOffset = expression.endOffset,
			)

			// Build a small IR block: first get KojaInitializer, then do the original call
			return builder.irBlock(expression = expression) {
				+irGetObject(initializerSymbol)
				+expression
			}
		}

		return expression
	}

	override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
		expression.transformChildrenVoid()

		val callee = expression.symbol.owner
		val isNamedBuilder = callee.constructedClass.name.asString() == "Builder"
		val isInServer = callee.constructedClass.parentClassOrNull?.name?.asString() == "Server"
		val isInRuntimePackage = callee.constructedClass.parentClassOrNull?.packageFqName?.asString() == "sh.ondr.kmcp.runtime"
		if (isNamedBuilder && isInServer && isInRuntimePackage) {
			val initializerSymbol = pluginContext.referenceClass(initializerClassId) ?: error("Could not find KojaInitializer")

			val builder =
				DeclarationIrBuilder(
					generatorContext = pluginContext,
					symbol = callee.symbol,
					startOffset = expression.startOffset,
					endOffset = expression.endOffset,
				)

			return builder.irBlock(expression = expression) {
				+irGetObject(initializerSymbol)
				+expression
			}
		}

		return expression
	}
}
