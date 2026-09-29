import 'dart:convert';
import 'package:app/models/chapter.dart';
import 'package:http/http.dart' as http;

import '../models/fiction.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080/api';

  Future<Fiction> getFiction(String id) async {
    final response = await http.get(
      Uri.parse('$baseUrl/fictions/$id'),
    );
    
    if (response.statusCode != 200) {
      throw Exception('Failed to load fiction');
    }

    return Fiction.fromJson(jsonDecode(response.body));
  }
  
  Future<List<Fiction>> getFictions() async {
    final response = await http.get(
      Uri.parse('$baseUrl/fictions'),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to load fictions: ${response.statusCode}');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map((json) => Fiction.fromJson(json))
        .toList();
  }

  Future<List<Fiction>> getFinishedFictions() async {
    final response = await http.get(
      Uri.parse('$baseUrl/fictions/finished'),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to load finished fictions: ${response.statusCode}');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map((json) => Fiction.fromJson(json))
        .toList();
  }

  Future<List<Chapter>> getChapters(String fictionId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/fictions/$fictionId/chapters'),
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to load chapters: ${response.statusCode}');
    }

    final List<dynamic> data = jsonDecode(response.body);

    return data
        .map((json) => Chapter.fromJson(json))
        .toList();
  }

  Future<Fiction> addFiction(String url) async {
    final response = await http.post(
      Uri.parse("$baseUrl/fictions"),
      headers: {
        "Content-Type": "text/plain",
      },
      body: url,
      );
      if (response.statusCode != 200) {
        throw Exception("Failed to add fiction: ${response.statusCode}");
      }
      return Fiction.fromJson(jsonDecode(response.body));
  }

  Future<Fiction> finishFiction(String fictionId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/fictions/$fictionId/finish'),
    );

    if (response.statusCode != 200) {
      throw Exception(
        'Failed to finish fiction: ${response.statusCode}',
      );
    }

    return Fiction.fromJson(jsonDecode(response.body));
  }

  Future<Fiction> updateReaderProgress(String fictionId, int chapter) async {
    final response = await http.post(
      Uri.parse('$baseUrl/fictions/$fictionId/progress'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(chapter),
    );

    if (response.statusCode != 200) {
      throw Exception(
        'Failed to update reader progress: ${response.statusCode}',
      );
    }

    return Fiction.fromJson(jsonDecode(response.body));
  }
 
  Future<void> removeFiction(String fictionId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/fictions/$fictionId/remove'),
    );

    if (response.statusCode != 200) {
      throw Exception(
        'Failed to remove fiction: ${response.statusCode}',
      );
    }
  }
  
}