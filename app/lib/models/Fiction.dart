class Fiction {
  final String id;
  final String ficLink;
  final String site;
  final String title;
  final String author;
  final int chapAmount;
  final int wordCount;
  final String description;

  const Fiction({
    required this.id,
    required this.ficLink,
    required this.site,
    required this.title,
    required this.author,
    required this.chapAmount,
    required this.wordCount,
    required this.description,
  });

  factory Fiction.fromJson(Map<String, dynamic> json) {
    return Fiction(
      id: json['id'],
      ficLink: json['ficLink'],
      site: json['site'],
      title: json['title'],
      author: json['author'],
      chapAmount: json['chapAmount'],
      wordCount: json['wordCount'],
      description: json['description'],
    );
  }
}