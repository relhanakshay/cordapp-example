package com.example.contract;

import com.example.state.IOUState;
import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.security.PublicKey;

import static com.example.contract.IOUContract.IOU_CONTRACT_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class IOUContractTests {
    static private final MockServices ledgerServices = new MockServices(ImmutableList.of("com.example.contract"));
    static private TestIdentity megaCorpIdentity = new TestIdentity(new CordaX500Name("MegaCorp", "London", "GB"));
    static private TestIdentity miniCorpIdentity = new TestIdentity(new CordaX500Name("MiniCorp", "London", "GB"));
    static private Party megaCorp = megaCorpIdentity.getParty();
    static private Party miniCorp = miniCorpIdentity.getParty();
    static private PublicKey megaCorpPubKey = megaCorpIdentity.getPublicKey();
    static private PublicKey miniCorpPubKey = miniCorpIdentity.getPublicKey();

    @Test
    public void transactionMustIncludeCreateCommand() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.fails();
                tx.command(ImmutableList.of(megaCorpPubKey, miniCorpPubKey), new IOUContract.Commands.Create());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveNoInputs() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.command(ImmutableList.of(megaCorpPubKey, miniCorpPubKey), new IOUContract.Commands.Create());
                tx.failsWith("No inputs should be consumed when issuing an IOU.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transactionMustHaveOneOutput() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.command(ImmutableList.of(megaCorpPubKey, miniCorpPubKey), new IOUContract.Commands.Create());
                tx.failsWith("Only one output state should be created.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.command(miniCorpPubKey, new IOUContract.Commands.Create());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void borrowerMustSignTransaction() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.command(megaCorpPubKey, new IOUContract.Commands.Create());
                tx.failsWith("All of the participants must be signers.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void lenderIsNotBorrower() {
        Integer iou = 1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, megaCorp, megaCorp));
                tx.command(ImmutableList.of(megaCorpPubKey, miniCorpPubKey), new IOUContract.Commands.Create());
                tx.failsWith("The lender and the borrower cannot be the same entity.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void cannotCreateNegativeValueIOUs() {
        Integer iou = -1;
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(IOU_CONTRACT_ID, new IOUState(iou, miniCorp, megaCorp));
                tx.command(ImmutableList.of(megaCorpPubKey, miniCorpPubKey), new IOUContract.Commands.Create());
                tx.failsWith("The IOU's value must be non-negative.");
                return null;
            });
            return null;
        }));
    }
}