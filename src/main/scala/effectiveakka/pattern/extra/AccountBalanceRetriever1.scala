package effectiveakka.pattern.extra
import akka.actor.{ Actor, ActorRef }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import effectiveakka.pattern._

class AccountBalanceRetriever1(cActor: ActorRef, sActor: ActorRef, mmActor: ActorRef) extends Actor{
  implicit val timeout: Timeout = 100 milliseconds
  implicit val ec: ExecutionContext = context.dispatcher

  def receive = {
    case GetCustomerAccountBalances(id) =>
      val f_checkings = cActor ? GetCustomerAccountBalances(id)
      val f_savings = sActor ? GetCustomerAccountBalances(id)
      val f_moneyMarkets = mmActor ? GetCustomerAccountBalances(id)
      val f_balances = for {
        cs <- f_checkings.mapTo[Option[List[(Long, BigDecimal)]]]
        ss <- f_savings.mapTo[Option[List[(Long, BigDecimal)]]]
        mms <- f_moneyMarkets.mapTo[Option[List[(Long, BigDecimal)]]]
      } yield AccountBalances(cs, ss, mms)
      f_balances map (sender ! _)
  }
}