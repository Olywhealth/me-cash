package com.mecash.controller;
import com.mecash.service.transfer.TransferService;

import com.mecash.entity.Transaction;
import com.mecash.model.reponse.TransactionResponse;
import com.mecash.model.request.TransferRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Transfers money from the caller's account to another account, converting between
     * currencies as needed. The response shows the transaction from the source perspective.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal String userEmail,
            @Valid @RequestBody TransferRequest request) {
        Transaction transaction = transferService.transfer(userEmail, request);
        TransactionResponse body =
                TransactionResponse.forAccount(transaction, transaction.getSourceAccountNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}
