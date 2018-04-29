package com.kish.spring.kotlintest

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Profile
import org.springframework.context.support.beans
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class KotlintestApplication

fun main(args: Array<String>) {
//    runApplication<KotlintestApplication>(*args)

    SpringApplicationBuilder()
            .sources(KotlintestApplication::class.java)
            .initializers( beans {
                bean {
                    SpringTransactionManager(ref())
                }
                bean{
                    ApplicationRunner {
                        val customerService = ref<CustomerService>()
                        arrayOf("Thej","Abirami","Shanaya","Kriti")
                                .map { Customer(name =it) }
                                .forEach{ customerService.insert(it)}

                        customerService.all().forEach{ println(it)}
                    }
                }


            })
            .run(*args)


}


object Customers : Table(){

    val id= long("id").autoIncrement().primaryKey()
    val name = varchar("name",255)

}



@RestController
class CustomerRestController (private val customerService: CustomerService){
    @GetMapping("/Customers")
    fun customers() = customerService.all()

    @GetMapping("/Customers/{id}")
    fun customer(@PathVariable id:Long) = customerService.byId(id)

}




@Service
@Transactional
class ExposedCustomerService (private val transactionTemplate: TransactionTemplate) : CustomerService,InitializingBean{
    override fun all(): Collection<Customer> =
            Customers.selectAll().map { Customer(it[Customers.name],it[Customers.id]) }

    override fun insert(c: Customer) {
        Customers.insert {
            it[Customers.name] = c.name
        }
    }

    override fun byId(id:Long) : Customer? = Customers.select() { Customers.id.eq(id) }
            .map { Customer(it[Customers.name],it[Customers.id]) }
            .firstOrNull()

    override fun afterPropertiesSet() {
        transactionTemplate.execute {
            SchemaUtils.create(Customers)
        }
    }

}


@Profile("jdbc")
@Service
@Transactional
class JdbcCustomerService(private val jdbcOperations: JdbcOperations):CustomerService{
    override fun byId(id: Long): Customer? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun all(): Collection<Customer> =
         this.jdbcOperations.query("SELECT * FROM CUSTOMERS") { rs, _ -> Customer(rs.getString("NAME"),rs.getLong("ID"))}

    override fun insert(c: Customer) {
        this.jdbcOperations.execute("INSERT INTO CUSTOMERS(NAME) VALUES (?)") {
            it.setString(1,c.name)
            it.execute()
        }
    }

}


interface CustomerService{
    fun all(): Collection<Customer>
    fun byId(id:Long) : Customer?
    fun insert(c: Customer)
}



data class Customer(val name:String,var id: Long? = null)