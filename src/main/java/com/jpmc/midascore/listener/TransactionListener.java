package com.jpmc.midascore.listener;

import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.foundation.Incentive;   // ✅ ADD THIS
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.repository.UserRepository;
import com.jpmc.midascore.repository.TransactionRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;   // ✅ ADD THIS

@Service
public class TransactionListener {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;   // ✅ ADD THIS

    public TransactionListener(UserRepository userRepository,
                               TransactionRepository transactionRepository,
                               RestTemplate restTemplate) {   // ✅ UPDATE
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    @KafkaListener(topics = "trader-updates", groupId = "midas-core-group")
    public void listen(Transaction txn) {

        UserRecord sender = userRepository.findById(txn.getSenderId()).orElse(null);
        UserRecord recipient = userRepository.findById(txn.getRecipientId()).orElse(null);

        if (sender == null || recipient == null) return;
        if (sender.getBalance() < txn.getAmount()) return;


        // ✅ CALL INCENTIVE API
        double incentiveAmount = 0;

        try {
            String url = "http://localhost:8080/incentive";

            Incentive response = restTemplate.postForObject(
                    url,
                    txn,
                    Incentive.class
            );

            if (response != null) {
                incentiveAmount = response.getAmount();
            }

        } catch (Exception e) {
            incentiveAmount = 0; // fail safe
        }

        // ✅ UPDATE BALANCES (IMPORTANT)
        sender.setBalance((float) (sender.getBalance() - txn.getAmount()));

        recipient.setBalance((float) (
                recipient.getBalance() + txn.getAmount() + incentiveAmount
        ));

        userRepository.save(sender);
        userRepository.save(recipient);

        // ✅ SAVE TRANSACTION WITH INCENTIVE
        TransactionRecord record = new TransactionRecord();
        record.setAmount((float) txn.getAmount());
        record.setIncentive(incentiveAmount);   // ✅ ADD THIS
        record.setSender(sender);
        record.setRecipient(recipient);
        record.setTimestamp(System.currentTimeMillis());

        transactionRepository.save(record);

        // (optional debug)
        userRepository.findByName("wilbur").ifPresent(u ->
                System.out.println("WILBUR BALANCE = " + u.getBalance())
        );
    }
}