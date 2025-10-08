package com.example.springbootjava.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ScheduledBackupService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledBackupService.class);

    @Autowired
    private DatabaseBackupService backupService;

    @Value("${backup.scheduled.enabled:true}")
    private boolean scheduledBackupEnabled;

    @Value("${backup.scheduled.retention-days:30}")
    private int retentionDays;

    @Value("${backup.scheduled.max-backups:50}")
    private int maxBackups;

    /**
     * Daily backup at 2:00 AM
     */
    @Scheduled(cron = "${backup.scheduled.daily-cron:0 0 2 * * ?}")
    public void performDailyBackup() {
        if (!scheduledBackupEnabled) {
            logger.debug("Scheduled backup is disabled");
            return;
        }

        try {
            logger.info("Starting scheduled daily backup");
            String backupPath = backupService.createBackup();
            logger.info("Scheduled daily backup completed: {}", backupPath);
            
            // Clean up old backups
            cleanupOldBackups();
            
        } catch (Exception e) {
            logger.error("Scheduled daily backup failed", e);
        }
    }

    /**
     * Weekly backup on Sundays at 3:00 AM
     */
    @Scheduled(cron = "${backup.scheduled.weekly-cron:0 0 3 * * SUN}")
    public void performWeeklyBackup() {
        if (!scheduledBackupEnabled) {
            logger.debug("Scheduled backup is disabled");
            return;
        }

        try {
            logger.info("Starting scheduled weekly backup");
            String backupPath = backupService.createBackup();
            logger.info("Scheduled weekly backup completed: {}", backupPath);
            
        } catch (Exception e) {
            logger.error("Scheduled weekly backup failed", e);
        }
    }

    /**
     * Monthly backup on the 1st at 4:00 AM
     */
    @Scheduled(cron = "${backup.scheduled.monthly-cron:0 0 4 1 * ?}")
    public void performMonthlyBackup() {
        if (!scheduledBackupEnabled) {
            logger.debug("Scheduled backup is disabled");
            return;
        }

        try {
            logger.info("Starting scheduled monthly backup");
            String backupPath = backupService.createBackup();
            logger.info("Scheduled monthly backup completed: {}", backupPath);
            
        } catch (Exception e) {
            logger.error("Scheduled monthly backup failed", e);
        }
    }

    /**
     * Cleanup old backups every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void cleanupOldBackups() {
        if (!scheduledBackupEnabled) {
            logger.debug("Scheduled backup is disabled");
            return;
        }

        try {
            logger.debug("Starting scheduled backup cleanup");
            int deletedCount = backupService.cleanupOldBackups(maxBackups);
            if (deletedCount > 0) {
                logger.info("Cleaned up {} old backup files", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("Scheduled backup cleanup failed", e);
        }
    }

    /**
     * Manual backup trigger
     */
    public String performManualBackup() throws IOException {
        logger.info("Starting manual backup");
        String backupPath = backupService.createBackup();
        logger.info("Manual backup completed: {}", backupPath);
        return backupPath;
    }

    /**
     * Get backup service status
     */
    public BackupStatus getBackupStatus() {
        try {
            var backups = backupService.listBackups();
            long totalSize = backups.stream().mapToLong(backup -> backup.getSize()).sum();
            
            return new BackupStatus(
                scheduledBackupEnabled,
                backups.size(),
                totalSize,
                retentionDays,
                maxBackups
            );
        } catch (Exception e) {
            logger.error("Failed to get backup status", e);
            return new BackupStatus(
                scheduledBackupEnabled,
                0,
                0,
                retentionDays,
                maxBackups
            );
        }
    }

    public static class BackupStatus {
        private final boolean enabled;
        private final int backupCount;
        private final long totalSizeBytes;
        private final int retentionDays;
        private final int maxBackups;

        public BackupStatus(boolean enabled, int backupCount, long totalSizeBytes, int retentionDays, int maxBackups) {
            this.enabled = enabled;
            this.backupCount = backupCount;
            this.totalSizeBytes = totalSizeBytes;
            this.retentionDays = retentionDays;
            this.maxBackups = maxBackups;
        }

        public boolean isEnabled() { return enabled; }
        public int getBackupCount() { return backupCount; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public double getTotalSizeMB() { return totalSizeBytes / (1024.0 * 1024.0); }
        public int getRetentionDays() { return retentionDays; }
        public int getMaxBackups() { return maxBackups; }
    }
}
