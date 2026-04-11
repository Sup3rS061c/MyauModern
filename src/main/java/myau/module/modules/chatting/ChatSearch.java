package myau.module.modules.chatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Chat Search System
 * Search through chat messages
 */
public class ChatSearch {
    
    private String searchQuery = "";
    private boolean useRegex = false;
    private boolean caseSensitive = false;
    private Pattern compiledPattern = null;
    
    private final List<String> allMessages = new ArrayList<>();
    private final List<String> filteredMessages = new ArrayList<>();
    private int currentResultIndex = -1;
    
    /**
     * Set search query
     */
    public void setQuery(String query) {
        this.searchQuery = query;
        updatePattern();
        filterMessages();
    }
    
    /**
     * Enable/disable regex search
     */
    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
        updatePattern();
        filterMessages();
    }
    
    /**
     * Enable/disable case sensitivity
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        updatePattern();
        filterMessages();
    }
    
    /**
     * Update the compiled pattern based on settings
     */
    private void updatePattern() {
        if (searchQuery.isEmpty()) {
            compiledPattern = null;
            return;
        }
        
        try {
            String pattern = searchQuery;
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            
            if (!useRegex) {
                // Escape special regex characters for literal search
                pattern = Pattern.quote(searchQuery);
            }
            
            compiledPattern = Pattern.compile(pattern, flags);
        } catch (PatternSyntaxException e) {
            // Invalid regex
            compiledPattern = null;
        }
    }
    
    /**
     * Filter messages based on search query
     */
    private void filterMessages() {
        filteredMessages.clear();
        currentResultIndex = -1;
        
        if (compiledPattern == null) {
            return;
        }
        
        for (int i = 0; i < allMessages.size(); i++) {
            String message = allMessages.get(i);
            if (compiledPattern.matcher(message).find()) {
                filteredMessages.add(message);
            }
        }
    }
    
    /**
     * Check if a message matches the search
     */
    public boolean matches(String message) {
        if (compiledPattern == null) return true;
        return compiledPattern.matcher(message).find();
    }
    
    /**
     * Add a message to the search index
     */
    public void addMessage(String message) {
        allMessages.add(message);
        
        // Keep only last 1000 messages to prevent memory issues
        if (allMessages.size() > 1000) {
            allMessages.remove(0);
        }
        
        // Update filtered list if searching
        if (compiledPattern != null && compiledPattern.matcher(message).find()) {
            filteredMessages.add(message);
        }
    }
    
    /**
     * Navigate to next result
     */
    public String nextResult() {
        if (filteredMessages.isEmpty()) return null;
        
        currentResultIndex++;
        if (currentResultIndex >= filteredMessages.size()) {
            currentResultIndex = 0;
        }
        
        return filteredMessages.get(currentResultIndex);
    }
    
    /**
     * Navigate to previous result
     */
    public String previousResult() {
        if (filteredMessages.isEmpty()) return null;
        
        currentResultIndex--;
        if (currentResultIndex < 0) {
            currentResultIndex = filteredMessages.size() - 1;
        }
        
        return filteredMessages.get(currentResultIndex);
    }
    
    /**
     * Get current result
     */
    public String getCurrentResult() {
        if (currentResultIndex < 0 || currentResultIndex >= filteredMessages.size()) {
            return null;
        }
        return filteredMessages.get(currentResultIndex);
    }
    
    /**
     * Get result count
     */
    public int getResultCount() {
        return filteredMessages.size();
    }
    
    /**
     * Get current result index
     */
    public int getCurrentResultIndex() {
        return currentResultIndex;
    }
    
    /**
     * Clear search
     */
    public void clear() {
        searchQuery = "";
        compiledPattern = null;
        filteredMessages.clear();
        currentResultIndex = -1;
    }
    
    /**
     * Clear all messages
     */
    public void clearMessages() {
        allMessages.clear();
        filteredMessages.clear();
        currentResultIndex = -1;
    }
    
    // Getters
    public String getSearchQuery() { return searchQuery; }
    public boolean isUseRegex() { return useRegex; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public List<String> getFilteredMessages() { return new ArrayList<>(filteredMessages); }
}
