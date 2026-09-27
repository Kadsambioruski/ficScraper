import 'dart:convert';
import 'package:app/models/chapter.dart';
import 'package:http/http.dart' as http;

import '../models/Fiction.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080/api';

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
  
}