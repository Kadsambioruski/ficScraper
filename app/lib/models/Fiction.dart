class Fiction {
  final String ficLink;
  final String site;
  final int ficID;
  final String title;
  final String author;
  final int chapAmount;
  final int wordCount;
  final String description;


  const Fiction({
    required this.ficLink,
    required this.site,
    required this.ficID,
    required this.title,
    required this.author,
    required this.chapAmount,
    required this.wordCount,
    required this.description
  });

  factory Fiction.fromJson(Map<String, dynamic> json) {
    return Fiction(
      ficLink: json['ficLink'],
      site: json['site'],
      ficID: json['ficID'],
      title: json['title'],
      author: json['author'],
      chapAmount: json['chapAmount'],
      wordCount: json['wordCount'],
      description: json['description'],
      );
  }
}