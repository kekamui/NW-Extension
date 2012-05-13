package org.nlogo.extensions.nw

import org.nlogo.api.Dump
import org.nlogo.api.Agent
import org.nlogo.api.AgentSet
import org.nlogo.api.Link
import org.nlogo.api.Turtle
import org.nlogo.api.Argument
import org.nlogo.api.ExtensionException
import org.nlogo.api.I18N
import org.nlogo.api.DefaultCommand
import org.nlogo.api.Syntax._
import org.nlogo.api.Context
import org.nlogo.api.TypeNames
import org.nlogo.api.DefaultReporter
import org.nlogo.api.Primitive

object PrimUtil {
  implicit def AgentSetToNetLogoAgentSet(agentSet: AgentSet) =
    agentSet.asInstanceOf[org.nlogo.agent.AgentSet]
  implicit def AgentToNetLogoAgent(agent: Agent) =
    agent.asInstanceOf[org.nlogo.agent.Agent]
  implicit def TurtleToNetLogoTurtle(turtle: Turtle) =
    turtle.asInstanceOf[org.nlogo.agent.Turtle]
  implicit def LinkToNetLogoLink(link: Link) =
    link.asInstanceOf[org.nlogo.agent.Link]
  implicit def AgentToRichAgent(agent: Agent) = new RichAgent(agent)
  class RichAgent(agent: Agent) {
    def requireAlive =
      if (agent.id != -1) // is alive
        agent
      else throw new ExtensionException(
        I18N.errors.get("org.nlogo.$common.thatAgentIsDead"))
  }
  implicit def AgentSetToRichAgentSet(agentSet: AgentSet) = new RichAgentSet(agentSet)
  class RichAgentSet(agentSet: AgentSet) {
    def isLinkSet = classOf[Link].isAssignableFrom(agentSet.`type`)
    def isTurtleSet = classOf[Turtle].isAssignableFrom(agentSet.`type`)
    lazy val world = agentSet.world.asInstanceOf[org.nlogo.agent.World]
    def isLinkBreed = (agentSet eq world.links) || world.isLinkBreed(agentSet)
    def isTurtleBreed = (agentSet eq world.turtles) || world.isBreed(agentSet)
    def requireTurtleSet =
      if (isTurtleSet) agentSet
      else throw new ExtensionException("Expected input to be a turtleset")
    def requireTurtleBreed =
      if (isTurtleBreed) agentSet
      else throw new ExtensionException("Expected input to be a turtle breed")
    def requireLinkSet =
      if (isLinkSet) agentSet
      else throw new ExtensionException("Expected input to be a linkset")
    def requireLinkBreed =
      if (isLinkBreed) agentSet
      else throw new ExtensionException(
        I18N.errors.get("org.nlogo.prim.etc.$common.expectedLastInputToBeLinkBreed"))
  }
  implicit def EnrichArgument(arg: Argument) = new RichArgument(arg)
  class RichArgument(arg: Argument) {
    def getStaticGraph = arg.get match {
      case g: StaticNetLogoGraph => g
      case _ => throw new ExtensionException(
        "Expected input to be a network snapshot")
    }
    def getGraph = arg.get match {
      case as: AgentSet          => new LiveNetLogoGraph(as.requireLinkBreed)
      case g: StaticNetLogoGraph => g
      case _ => throw new ExtensionException(
        "Expected input to be either a linkset or a network snapshot")
    }
  }
}

trait NetworkPrim extends Primitive {
  val primitiveName: String
  val args: Product

  def argsSyntax = args.productIterator
    .collect { case (typeConst: Int, _, _) => typeConst }
    .toArray(manifest[Int])

  override def getSyntax = commandSyntax(argsSyntax, getAgentClassString)

  private val i = Iterator.from(0)
  case class Arg[T](typeConst: Int) {
    val index = i.next
    def get(implicit argsArray: Array[Argument]) = {
      val argument = argsArray(index)
      val obj = argument.get
      try { obj.asInstanceOf[T] }
      catch {
        case (_: ClassCastException) => throw new org.nlogo.api.ExtensionException(
          "Expected this input to be " + TypeNames.aName(typeConst) + " but got " +
            (if (obj == org.nlogo.api.Nobody$.MODULE$) "NOBODY"
            else "the " + TypeNames.name(obj) + " " + Dump.logoObject(obj)) +
            " instead.")
      }
    }
  }  
}

trait NetworkCommand extends DefaultCommand with NetworkPrim {
  def perform(context: Context)(implicit argsArray: Array[Argument])
  override def perform(argsArray: Array[Argument], context: Context) {
    perform(context)(argsArray)
  }
}

trait NetworkReporter extends DefaultReporter with NetworkPrim {
  def report(context: Context)(implicit argsArray: Array[Argument]): AnyRef
  override def report(argsArray: Array[Argument], context: Context) = {
    report(context)(argsArray)
  }
}

