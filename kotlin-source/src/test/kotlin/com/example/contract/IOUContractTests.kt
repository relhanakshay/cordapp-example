package com.example.contract

import com.example.contract.IOUContract.Companion.IOU_CONTRACT_ID
import com.example.state.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IOUContractTests {
    private val ledgerServices = MockServices(listOf("com.example.contract"))
    private val megaCorpIdentity = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorpIdentity = TestIdentity(CordaX500Name("MiniCorp", "London", "GB"))
    private val megaCorp = megaCorpIdentity.party
    private val miniCorp = miniCorpIdentity.party
    private val megaCorpPubKey = megaCorpIdentity.publicKey
    private val miniCorpPubKey = miniCorpIdentity.publicKey

    @Test
    fun `transaction must include Create command`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                fails()
                command(listOf(megaCorpPubKey, miniCorpPubKey), IOUContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                command(listOf(megaCorpPubKey, miniCorpPubKey), IOUContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                command(listOf(megaCorpPubKey, miniCorpPubKey), IOUContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                command(miniCorpPubKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                command(megaCorpPubKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        val iou = 1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, megaCorp, megaCorp))
                command(listOf(megaCorpPubKey, miniCorpPubKey), IOUContract.Commands.Create())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        val iou = -1
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iou, miniCorp, megaCorp))
                command(listOf(megaCorpPubKey, miniCorpPubKey), IOUContract.Commands.Create())
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }
}