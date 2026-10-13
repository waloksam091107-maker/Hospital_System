// search.js

function searchSuggestions() {
    const query = $("#searchInput").val();
    if (query) {
        $.ajax({
            url: '/find',
            type: 'GET',
            data: { search: query },
            success: function(data) {
                const suggestions = JSON.parse(data);
                let suggestionsList = '';
                suggestions.forEach(function(suggestion) {
                    suggestionsList += `<li onclick="selectSuggestion('${suggestion}')">${suggestion}</li>`;
                });
                $("#suggestions").html(suggestionsList).show();
            }
        });
    } else {
        $("#suggestions").hide();
    }
}

function showDropdown() {
    $("#suggestions").show();
}

function hideDropdown() {
    setTimeout(() => {
        $("#suggestions").hide();
    }, 200); // 延迟隐藏，以便用户可以点击选项
}

function selectSuggestion(suggestion) {
    $("#searchInput").val(suggestion);
    $("#suggestions").hide();
}
