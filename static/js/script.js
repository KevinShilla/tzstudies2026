document.getElementById('searchInput').addEventListener('input', function() {
    const filter = this.value.toLowerCase();
    const cards = document.querySelectorAll('.exam-card');
  
    cards.forEach(card => {
      const text = card.textContent.toLowerCase();
      card.style.display = text.includes(filter) ? '' : 'none';
    });
});


document.getElementById('askButton').addEventListener('click', function() {
    let query = document.getElementById('aiQuery').value;
    let responseDiv = document.getElementById('aiResponse');
    responseDiv.textContent = "Loading answer...";

    fetch('/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: query })
    })
    .then(response => response.json())
    .then(data => {
        if (data.answer) {
            responseDiv.textContent = data.answer;
        } else if (data.error) {
            responseDiv.textContent = "Error: " + data.error;
        }
    })
    .catch(error => {
        responseDiv.textContent = "Error: " + error;
    });
});
