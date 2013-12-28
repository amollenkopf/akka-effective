package effectiveakka.pattern
import akka.actor.Actor;

case class GetCustomerAccountBalances(id: Long)

case class AccountBalances(
    val checking: Option[List[(Long, BigDecimal)]],
    val savings: Option[List[(Long, BigDecimal)]],
    val moneyMarkets: Option[List[(Long, BigDecimal)]])
case class CheckingAccountBalances(val balances: Option[List[(Long, BigDecimal)]])
case class SavingAccountBalances(val balances: Option[List[(Long, BigDecimal)]])
case class MoneyMarketAccountBalances(val balances: Option[List[(Long, BigDecimal)]])

trait CheckingAccountsProxy extends Actor
trait SavingAccountsProxy extends Actor
trait MoneyMarketAccountsProxy extends Actor