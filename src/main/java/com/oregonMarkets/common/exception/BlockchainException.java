package com.oregonMarkets.common.exception;

import com.oregonMarkets.common.response.ResponseCode;

public class BlockchainException extends BusinessException {

    public BlockchainException(String message) {
        super(ResponseCode.BLOCKCHAIN_ERROR, message);
    }

    public BlockchainException(String message, Throwable cause) {
        super(ResponseCode.BLOCKCHAIN_ERROR, message, cause);
    }
}