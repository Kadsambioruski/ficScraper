import 'package:flutter/material.dart';
import 'package:app/models/Fiction.dart';


final List<Fiction> fictions = [
  Fiction(
    ficLink: 'https://example.com',
    site: 'AO3',
    ficID: 1,
    title: 'My First Fic',
    author: 'Bob',
    chapAmount: 10,
    wordCount: 50000,
    description: 'A cool story.',
  ),
  Fiction(
    ficLink: 'https://example.com',
    site: 'Fanfiction',
    ficID: 2,
    title: 'Another Fic',
    author: 'Alice',
    chapAmount: 20,
    wordCount: 100000,
    description: 'Another cool story.',
  ),
];
void main() {
  runApp(
    MaterialApp(
      home: Scaffold(
        body: 
          GridView.builder(
            padding: const EdgeInsets.all(10),
            itemCount: fictions.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 10,
              mainAxisSpacing: 10,
            ),
            itemBuilder: (context, index) {
              return FicCard(fiction: fictions[index]);
            }
          ),
        appBar: AppBar(
          title: Row(
            children: [
              IconButton(
              onPressed: () {
                print("Menu Clicked!");
                }, 
              icon: const Icon(Icons.menu)
              ),
              const Text(
                "FicScraper",
                style: TextStyle(
                  color: Colors.black
                ), 
              ),
            ],
            
            ),
          backgroundColor: Colors.grey,
          ),
      ),
    ),
  );
}

class FicCard extends StatelessWidget {
  final Fiction fiction;

  const FicCard({
    required this.fiction,
    super.key
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 200,
      padding: const EdgeInsets.all(20),
      margin: const EdgeInsets.only(right: 10),
      color: Colors.amber,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(fiction.title),
          Text(fiction.author),
          Text(fiction.site),
          Text('${fiction.chapAmount} chapters'),
          Text('${fiction.wordCount} words')

        ],
      ),
    );
  }
}