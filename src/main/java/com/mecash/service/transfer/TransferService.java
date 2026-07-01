package com.mecash.service.transfer;

import com.mecash.entity.Transaction;
import com.mecash.model.request.TransferRequest;

public interface TransferService {
    Transaction transfer(String userEmail, TransferRequest request);
}
